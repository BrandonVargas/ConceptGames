#!/usr/bin/env python3
"""
A tiny stand-in for the Gemini API, for developing ConceptGames without an API key or quota.

It answers the three endpoints the app uses:
  GET  /v1beta/models                                   -> a model list (also used by "Test and save")
  POST /v1beta/models/<model>:generateContent           -> the Google Search grounded brief
  POST /v1beta/models/<model>:streamGenerateContent     -> the study pack, streamed slowly over SSE,
                                                           with thought summaries first

The pack it streams is the bundled Photosynthesis sample, whatever the topic.

Usage:
  python3 tools/mock-gemini/mock_gemini.py            # listens on 127.0.0.1:8787
  adb reverse tcp:8787 tcp:8787                       # lets the emulator or a USB phone reach it as localhost
  # local.properties (debug builds only):  gemini.baseUrl=http://localhost:8787/v1beta
  # then enter any key in the app, e.g. "mock-key" ("bad" simulates an invalid key)

Environment variables:
  MOCK_GROUNDING=nofree|fallback|ok   nofree (default): search grounding is not in the free tier for any model;
                                      fallback: only gemini-2.5-flash can search; ok: every model can search.
  MOCK_RATE_LIMIT_ONCE=1              the first pack request gets a 429 asking to retry in 6 s.
  MOCK_SPEED=1.0                      >1 streams faster, <1 slower.
"""
import json
import os
import pathlib
import re
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ROOT = pathlib.Path(__file__).resolve().parents[2]
SAMPLE = ROOT / "app/src/main/assets/sample_packs/photosynthesis_en.json"
PACK_JSON = json.dumps(json.loads(SAMPLE.read_text())["content"], ensure_ascii=False)

GROUNDING = os.environ.get("MOCK_GROUNDING", "nofree")
SPEED = float(os.environ.get("MOCK_SPEED", "1.0"))
state = {"rate_limit_pending": os.environ.get("MOCK_RATE_LIMIT_ONCE") == "1"}

THOUGHTS = [
    "**Reading the sources**\n\nI'm going through the Wikipedia extracts to find the key ideas.",
    "**Choosing the key concepts**\n\nPicking terms that matter most for a learner.",
    "**Writing questions and clues**\n\nDrafting plausible distractors and short crossword clues.",
]


def sleep(seconds):
    time.sleep(seconds / SPEED)


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        print(f"[mock-gemini] {self.command} {self.path} -> {fmt % args}")

    # --- helpers -------------------------------------------------------------------------------------------------
    def send_json(self, status, payload):
        body = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def error(self, status, api_status, message, details=None):
        self.send_json(status, {"error": {"code": status, "status": api_status, "message": message, "details": details or []}})

    def key_ok(self):
        key = self.headers.get("x-goog-api-key", "")
        if not key or key == "bad":
            self.error(400, "INVALID_ARGUMENT", "API key not valid. Please pass a valid API key.", [{"reason": "API_KEY_INVALID"}])
            return False
        return True

    def read_body(self):
        length = int(self.headers.get("Content-Length", "0"))
        return json.loads(self.rfile.read(length) or b"{}")

    # --- endpoints -----------------------------------------------------------------------------------------------
    def do_GET(self):
        if not self.key_ok():
            return
        if self.path.startswith("/v1beta/models"):
            names = ["gemini-flash-latest", "gemini-flash-lite-latest", "gemini-2.5-flash"]
            self.send_json(200, {"models": [{"name": f"models/{n}", "supportedGenerationMethods": ["generateContent"]} for n in names]})
        else:
            self.error(404, "NOT_FOUND", f"{self.path} not found")

    def do_POST(self):
        if not self.key_ok():
            return
        match = re.match(r"^/v1beta/models/([^:]+):(generateContent|streamGenerateContent)", self.path)
        if not match:
            return self.error(404, "NOT_FOUND", f"{self.path} not found")
        model, method = match.groups()
        request = self.read_body()
        if method == "generateContent":
            self.grounded(model, request)
        else:
            self.stream_pack()

    def grounded(self, model, request):
        if not request.get("tools"):
            return self.error(400, "INVALID_ARGUMENT", "This mock only grounds requests with the google_search tool.")
        allowed = GROUNDING == "ok" or (GROUNDING == "fallback" and model == "gemini-2.5-flash")
        if not allowed:
            return self.error(
                429,
                "RESOURCE_EXHAUSTED",
                f"Quota exceeded for metric: generativelanguage.googleapis.com/search_grounding_request_free_tier, limit: 0, model: {model}",
                [{"@type": "type.googleapis.com/google.rpc.QuotaFailure",
                  "violations": [{"quotaMetric": "generativelanguage.googleapis.com/search_grounding_request_free_tier", "quotaValue": "0"}]}],
            )
        sleep(2)
        self.send_json(200, {"candidates": [{
            "content": {"parts": [{"text": "Photosynthesis converts light energy into chemical energy stored in sugars. "
                                           "It takes place in chloroplasts and releases oxygen as a by-product."}]},
            "finishReason": "STOP",
            "groundingMetadata": {"groundingChunks": [
                {"web": {"uri": "https://www.britannica.com/science/photosynthesis", "title": "britannica.com"}},
                {"web": {"uri": "https://www.khanacademy.org/science/biology/photosynthesis-in-plants", "title": "khanacademy.org"}},
            ]},
        }]})

    def stream_pack(self):
        if state["rate_limit_pending"]:
            state["rate_limit_pending"] = False
            return self.error(
                429, "RESOURCE_EXHAUSTED", "Quota exceeded for metric: generate_content_free_tier_requests, limit: 10",
                [{"@type": "type.googleapis.com/google.rpc.RetryInfo", "retryDelay": "6s"}],
            )
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream")
        self.send_header("Cache-Control", "no-cache")
        self.send_header("Connection", "close")
        self.end_headers()
        try:
            for thought in THOUGHTS:
                sleep(2)
                self.event({"content": {"parts": [{"text": thought, "thought": True}]}})
            chunks = [PACK_JSON[i:i + 350] for i in range(0, len(PACK_JSON), 350)]
            for index, chunk in enumerate(chunks):
                sleep(0.25)
                candidate = {"content": {"parts": [{"text": chunk}]}}
                if index == len(chunks) - 1:
                    candidate["finishReason"] = "STOP"
                self.event(candidate)
        except (BrokenPipeError, ConnectionResetError):
            print("[mock-gemini] client cancelled the stream")
        self.close_connection = True

    def event(self, candidate):
        self.wfile.write(f"data: {json.dumps({'candidates': [candidate]})}\r\n\r\n".encode())
        self.wfile.flush()


if __name__ == "__main__":
    server = ThreadingHTTPServer(("127.0.0.1", 8787), Handler)
    print(f"[mock-gemini] grounding={GROUNDING} on http://127.0.0.1:8787/v1beta (run: adb reverse tcp:8787 tcp:8787)")
    server.serve_forever()
