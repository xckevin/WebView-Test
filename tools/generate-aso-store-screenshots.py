#!/usr/bin/env python3
"""Generate Google Play screenshots with the installed ASO screenshot skill."""

from __future__ import annotations

import importlib.util
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SKILL_DIR = Path.home() / ".codex" / "skills" / "aso-store-screenshots"
COMPOSE_PATH = SKILL_DIR / "compose.py"
FEATURE_PATH = SKILL_DIR / "generate_feature_graphic.py"

OUTPUT_ROOT = ROOT / "screenshots" / "aso-google-play"
FINAL_ROOT = OUTPUT_ROOT / "final"
PLAY_LISTINGS = ROOT / "app" / "src" / "main" / "play" / "listings"
RAW = ROOT / "docs" / "play-store" / "screenshots" / "raw"

BRAND_COLOR = "#163B45"
FONT_CANDIDATES = [
    Path("/Library/Fonts/Arial Unicode.ttf"),
    Path("/System/Library/Fonts/Supplemental/Arial Unicode.ttf"),
    Path("/System/Library/Fonts/SFNS.ttf"),
]


def load_module(path: Path, module_name: str):
    spec = importlib.util.spec_from_file_location(module_name, path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load module from {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def font_path() -> str:
    for candidate in FONT_CANDIDATES:
        if candidate.exists():
            return str(candidate)
    raise FileNotFoundError("No compatible local font found.")


def slug(value: str) -> str:
    keep = []
    for char in value.lower():
        if char.isalnum():
            keep.append(char)
        elif keep and keep[-1] != "-":
            keep.append("-")
    return "".join(keep).strip("-")


LOCALES = {
    "en-US": [
        ("LOAD", "WEBVIEW PAGES", "workbench-en-clean.png"),
        ("INSPECT", "LIVE SESSIONS", "overview-en.png"),
        ("TRACE", "CONSOLE ISSUES", "logs-en.png"),
        ("TUNE", "TEST SETTINGS", "settings-en.png"),
    ],
    "zh-CN": [
        ("加载", "WEBVIEW 页面", "workbench-zh-CN.png"),
        ("检查", "实时会话", "overview-zh-CN.png"),
        ("追踪", "CONSOLE 问题", "logs-en.png"),
        ("调整", "测试设置", "settings-en.png"),
    ],
    "ja-JP": [
        ("読み込む", "WEBVIEWページ", "workbench-en-clean.png"),
        ("検査", "ライブセッション", "overview-en.png"),
        ("追跡", "CONSOLEの問題", "logs-en.png"),
        ("調整", "テスト設定", "settings-en.png"),
    ],
    "es-ES": [
        ("CARGA", "PAGINAS WEBVIEW", "workbench-en-clean.png"),
        ("INSPECCIONA", "SESIONES EN VIVO", "overview-en.png"),
        ("RASTREA", "PROBLEMAS CONSOLE", "logs-en.png"),
        ("AJUSTA", "PRUEBAS WEBVIEW", "settings-en.png"),
    ],
    "fr-FR": [
        ("CHARGE", "PAGES WEBVIEW", "workbench-en-clean.png"),
        ("INSPECTE", "SESSIONS EN DIRECT", "overview-en.png"),
        ("SUIVIS", "PROBLEMES CONSOLE", "logs-en.png"),
        ("AJUSTE", "TESTS WEBVIEW", "settings-en.png"),
    ],
    "de-DE": [
        ("LADE", "WEBVIEW-SEITEN", "workbench-en-clean.png"),
        ("PRUFE", "LIVE-SITZUNGEN", "overview-en.png"),
        ("VERFOLGE", "CONSOLE-PROBLEME", "logs-en.png"),
        ("STEUERE", "WEBVIEW-TESTS", "settings-en.png"),
    ],
    "pt-PT": [
        ("CARREGA", "PAGINAS WEBVIEW", "workbench-en-clean.png"),
        ("INSPECIONA", "SESSOES EM DIRETO", "overview-en.png"),
        ("RASTREIA", "PROBLEMAS CONSOLE", "logs-en.png"),
        ("AJUSTA", "TESTES WEBVIEW", "settings-en.png"),
    ],
    "ru-RU": [
        ("ОТКРЫВАЙ", "WEBVIEW-СТРАНИЦЫ", "workbench-en-clean.png"),
        ("ПРОВЕРЯЙ", "LIVE-СЕССИИ", "overview-en.png"),
        ("ОТСЛЕЖИВАЙ", "CONSOLE-ОШИБКИ", "logs-en.png"),
        ("НАСТРАИВАЙ", "WEBVIEW-ТЕСТЫ", "settings-en.png"),
    ],
}


def main() -> None:
    if not COMPOSE_PATH.exists():
        raise FileNotFoundError(f"Skill compose.py not found: {COMPOSE_PATH}")
    if not FEATURE_PATH.exists():
        raise FileNotFoundError(f"Skill generate_feature_graphic.py not found: {FEATURE_PATH}")

    compose = load_module(COMPOSE_PATH, "aso_compose")
    feature = load_module(FEATURE_PATH, "aso_feature")
    selected_font = font_path()
    compose.FONT_PATH = selected_font
    feature.FONT_PATH = selected_font

    FINAL_ROOT.mkdir(parents=True, exist_ok=True)

    for locale, benefits in LOCALES.items():
        locale_dir = OUTPUT_ROOT / locale
        locale_final = FINAL_ROOT / locale
        locale_dir.mkdir(parents=True, exist_ok=True)
        locale_final.mkdir(parents=True, exist_ok=True)

        play_graphics = PLAY_LISTINGS / locale / "graphics"
        play_shots = play_graphics / "phone-screenshots"
        play_feature = play_graphics / "feature-graphic"
        play_shots.mkdir(parents=True, exist_ok=True)
        play_feature.mkdir(parents=True, exist_ok=True)

        for index, (verb, desc, raw_name) in enumerate(benefits, start=1):
            source = RAW / raw_name
            if not source.exists():
                raise FileNotFoundError(f"Missing raw screenshot: {source}")

            benefit_slug = slug(f"{index:02d}-{verb}-{desc}") or f"{index:02d}"
            work_dir = locale_dir / benefit_slug
            work_dir.mkdir(parents=True, exist_ok=True)
            scaffold = work_dir / "scaffold.png"
            final = locale_final / f"{index:02d}-{benefit_slug}.png"

            compose.compose(
                BRAND_COLOR,
                verb,
                desc,
                str(source),
                str(scaffold),
                "play-store-android",
            )
            shutil.copyfile(scaffold, final)
            shutil.copyfile(final, play_shots / f"{index}.png")

        feature_source = RAW / benefits[0][2]
        feature_out = locale_dir / "feature-graphic.png"
        feature.build_feature_graphic(
            BRAND_COLOR,
            benefits[0][0],
            benefits[0][1],
            str(feature_source),
            str(feature_out),
        )
        shutil.copyfile(feature_out, play_feature / "1.png")

    print(f"Generated ASO screenshots in {OUTPUT_ROOT}")
    print(f"Synced Play assets under {PLAY_LISTINGS}")


if __name__ == "__main__":
    main()
