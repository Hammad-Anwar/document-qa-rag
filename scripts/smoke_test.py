"""Exercise real HTTP, gRPC, PDF/TXT, embeddings, pgvector and local LLM.

Run inside the AI container: python /workspace/scripts/smoke_test.py
Creates small demo documents and history, retained for inspection.
"""
import io
import json
import os
import time
import uuid
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from pypdf import PdfWriter
from pypdf.generic import DictionaryObject, NameObject, DecodedStreamObject

BASE = os.getenv("SMOKE_BASE_URL", "http://backend:8080")


def request(path, data=None, content_type=None):
    headers = {"Content-Type": content_type} if content_type else {}
    try:
        with urlopen(Request(BASE + path, data=data, headers=headers), timeout=330) as response:
            return response.status, json.loads(response.read())
    except HTTPError as error:
        return error.code, json.loads(error.read())


def upload(filename, content):
    boundary = "documentqa" + uuid.uuid4().hex
    body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{filename}\"\r\n"
            "Content-Type: application/octet-stream\r\n\r\n").encode() + content + f"\r\n--{boundary}--\r\n".encode()
    return request("/api/documents", body, "multipart/form-data; boundary=" + boundary)


def ask(doc_id, question):
    return request(f"/api/documents/{doc_id}/questions", json.dumps({"question": question}).encode(), "application/json")


def pdf_bytes():
    writer = PdfWriter()
    page = writer.add_blank_page(width=612, height=792)
    font = DictionaryObject({NameObject("/Type"): NameObject("/Font"),
        NameObject("/Subtype"): NameObject("/Type1"), NameObject("/BaseFont"): NameObject("/Helvetica")})
    page[NameObject("/Resources")] = DictionaryObject({NameObject("/Font"):
        DictionaryObject({NameObject("/F1"): writer._add_object(font)})})
    stream = DecodedStreamObject()
    stream.set_data(b"BT /F1 12 Tf 50 700 Td (DNS translates domain names into IP addresses.) Tj ET")
    page[NameObject("/Contents")] = writer._add_object(stream)
    result = io.BytesIO()
    writer.write(result)
    return result.getvalue()


def main():
    for _ in range(30):
        try:
            code, ping = request("/api/health/ai")
            if code == 200:
                break
        except (URLError, TimeoutError):
            pass
        time.sleep(2)
    else:
        raise AssertionError("Backend did not become ready")
    assert ping["status"] == "UP"
    code, doc = upload("networking.txt", b"TCP provides reliable delivery using acknowledgements and retransmissions. UDP does not guarantee delivery.")
    assert code == 201 and doc["status"] == "READY", (code, doc)
    doc_id = doc["id"]
    assert request("/api/documents/" + doc_id)[1]["chunkCount"] > 0
    code, answer = ask(doc_id, "How does TCP provide reliable delivery?")
    assert code == 201 and answer["sources"], (code, answer)
    assert any(term in answer["answer"].lower() for term in ["acknowledg", "retransmi"]), answer
    assert all("TCP" in source["content"] for source in answer["sources"])
    code, history = request(f"/api/documents/{doc_id}/questions")
    assert code == 200 and history[0]["id"] == answer["id"]
    code, unsupported = ask(doc_id, "What is the secret access code?")
    assert code == 201 and "could not find sufficient information" in unsupported["answer"].lower(), unsupported
    assert unsupported["sources"] == []
    assert ask(doc_id, " ")[0] == 400
    assert request("/api/documents/" + str(uuid.uuid4()))[0] == 404
    assert upload("x.exe", b"text")[0] == 400
    assert upload("empty.txt", b"")[0] == 400
    assert upload("oversize.txt", b" " * (10 * 1024 * 1024 + 1))[0] == 413
    code, failed = upload("invalid.pdf", b"not a PDF")
    assert code == 422 and failed["documentId"], (code, failed)
    assert request("/api/documents/" + failed["documentId"])[1]["status"] == "FAILED"
    assert ask(failed["documentId"], "why?")[0] == 409
    # Above gRPC's default 4 MiB limit; only a short excerpt remains after stripping padding.
    code, large = upload("large.txt", b"TCP uses acknowledgements." + b" " * (4 * 1024 * 1024))
    assert code == 201 and large["status"] == "READY", (code, large)
    code, pdf = upload("dns.pdf", pdf_bytes())
    assert code == 201 and pdf["status"] == "READY", (code, pdf)
    code, pdf_answer = ask(pdf["id"], "What does DNS do?")
    assert code == 201 and pdf_answer["sources"] and pdf_answer["sources"][0]["page"] == 1, (code, pdf_answer)
    assert "IP" in pdf_answer["answer"] or "ip" in pdf_answer["answer"], pdf_answer
    assert "TCP" not in " ".join(source["content"] for source in pdf_answer["sources"])
    print(json.dumps({"result": "PASS", "txtDocumentId": doc_id, "pdfDocumentId": pdf["id"],
        "txtAnswer": answer["answer"], "pdfAnswer": pdf_answer["answer"], "checks":
        "ping, TXT/PDF, embeddings, pgvector, model answers, sources, history, isolation, insufficient context, validation, FAILED state, 413, >4 MiB gRPC"}, indent=2))


if __name__ == "__main__":
    main()
