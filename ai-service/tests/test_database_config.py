import importlib

import pytest

from app.config import Settings


def test_import_without_database_configuration_is_safe(monkeypatch):
    monkeypatch.delenv("DATABASE_URL", raising=False)
    assert importlib.import_module("app.config").Settings is Settings


def test_missing_database_url_fails_without_a_credential_fallback(monkeypatch):
    monkeypatch.delenv("DATABASE_URL", raising=False)
    with pytest.raises(ValueError, match="DATABASE_URL"):
        Settings()


@pytest.mark.parametrize("value", ["", " ", "\t\n"])
def test_blank_database_url_fails_before_connecting(value):
    with pytest.raises(ValueError, match="DATABASE_URL"):
        Settings(database_url=value)


def test_database_url_is_read_when_settings_are_constructed(monkeypatch):
    monkeypatch.setenv("DATABASE_URL", "postgresql://localhost:5432/first")
    assert Settings().database_url == "postgresql://localhost:5432/first"
    monkeypatch.setenv("DATABASE_URL", "postgresql://localhost:5432/second")
    assert Settings().database_url == "postgresql://localhost:5432/second"


def test_explicit_url_works_without_environment_and_keeps_encoded_values(monkeypatch):
    monkeypatch.delenv("DATABASE_URL", raising=False)
    url = "postgresql://fixture:fixture%40value@localhost:5432/test"
    assert Settings(database_url=url).database_url == url


def test_explicit_url_overrides_environment(monkeypatch):
    monkeypatch.setenv("DATABASE_URL", "postgresql://localhost:5432/environment")
    assert Settings(database_url="postgresql://localhost:5432/explicit").database_url == "postgresql://localhost:5432/explicit"
