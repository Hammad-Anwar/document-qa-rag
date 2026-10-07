from __future__ import annotations

import sys
from pathlib import Path


AI_SERVICE_ROOT = Path(__file__).resolve().parents[1]
GENERATED_ROOT = AI_SERVICE_ROOT / "app" / "generated"

sys.path.insert(0, str(AI_SERVICE_ROOT))
sys.path.insert(0, str(GENERATED_ROOT))
