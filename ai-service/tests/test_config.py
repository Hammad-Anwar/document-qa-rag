import pytest

from app.config import Settings
from app.rag.chain import create_chat_model


def test_model_uses_configured_context_size():
    model = create_chat_model(Settings(model_context_tokens=8192))
    assert model.num_ctx == 8192


@pytest.mark.parametrize("size", [1024, 65536])
def test_context_size_is_bounded(size):
    with pytest.raises(ValueError, match="context"):
        Settings(model_context_tokens=size)
