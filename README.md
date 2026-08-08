<img src="./frontend/public/logo/logo-full.svg" alt="QuantEdge" height="40" />

AI-powered stock research and simulated trading platform.

See [CLAUDE.md](./CLAUDE.md) for the full project spec, tech stack, and workflow rules.

## Setup

After cloning, run these once from the repo root:

```bash
npm install          # installs lefthook, commitlint, prettier at root
                      # and auto-runs `lefthook install` via postinstall
cd frontend && npm install
cd ../backend && ./mvnw -q -DskipTests install
```

This wires up the git hooks (pre-commit formatting, commit-msg linting,
pre-push checks) documented in [CLAUDE.md § Tooling](./CLAUDE.md#tooling).

If `npm install` reports pending install scripts under `npm warn allow-scripts`,
approve lefthook's postinstall so hooks actually get installed:

```bash
npm approve-scripts lefthook
```

## Retrieval Evaluation

Phase 5's RAG layer (`backend/src/main/java/com/quantedge/backend/rag`) grounds the chat agent's
`queryKnowledgeBase` tool in a Qdrant Cloud vector store over a **frozen fixture corpus**: 20 news
articles + 14 research notes (`backend/src/main/resources/rag/*.json`), chosen frozen rather than
live so the gold-set labels below stay valid across runs. Embeddings are local ONNX
`all-MiniLM-L6-v2` (384-dim) — Groq has no embeddings endpoint, so chat and embeddings
intentionally use different models.

**Gold set**: 50 hand-labeled question → source-document pairs
(`backend/src/main/resources/rag/gold_set.json`), 30 from the news corpus and 20 from the
research-notes corpus, one gold document per question. Reproduce any row with:

```bash
cd backend
./mvnw -q compile -DskipTests
./mvnw -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
java -cp "target/classes;$(cat /tmp/cp.txt)" com.quantedge.backend.rag.eval.EvalRunner \
    <FIXED|RECURSIVE|SEMANTIC> [chunkSize=800] [chunkOverlap=120]
```

Requires `QDRANT_URL` / `QDRANT_API_KEY` env vars. Each run ingests the corpus into its own Qdrant
collection (`quantedge_eval_<label>`), so combos never contaminate each other, and scores
Recall@1/5/10 and MRR (`EvalMetrics`) against the gold set. This branch covers the dense-retrieval
baseline across chunking strategies; phase-5/rag-technique-experiments adds hybrid (dense+BM25)
and LLM-rerank rows on top.

| Config (chunking / rerank / retrieval mode / embedding model) | Recall@1 | Recall@5 | Recall@10 | MRR | Notes |
| --- | --- | --- | --- | --- | --- |
| FIXED / no-rerank / dense / all-MiniLM-L6-v2 (baseline) | 0.900 | 1.000 | 1.000 | 0.950 | |
| RECURSIVE / no-rerank / dense / all-MiniLM-L6-v2 | 0.900 | 1.000 | 1.000 | 0.950 | Same as fixed-size at this corpus size — see Notes below the table. |
| SEMANTIC / no-rerank / dense / all-MiniLM-L6-v2 | 0.900 | 1.000 | 1.000 | 0.950 | Produced 45 chunks vs 34 for fixed/recursive (finer-grained splits); no recall change at this corpus size. |

**Why every row is nearly identical**: the corpus is intentionally small (34–45 chunks across 34
source documents) and the gold questions are each answerable from one clearly-distinguishable
document, so dense retrieval alone already saturates Recall@5/@10 at 1.000 — there's no room left
for chunking to improve on. The one consistent miss (Recall@1 = 0.900, 5/50 questions) is a
technique-independent ceiling: those 5 questions each have a close semantic competitor among the
other documents that occasionally out-scores the correct one for the #1 spot, not something
chunking strategy fixes at this scale. This is an honest negative result, kept in the table rather
than omitted, per this harness's rule against only reporting positive deltas. A larger, noisier
corpus (e.g. full live news volume) is where chunking strategy would be expected to actually
differentiate.
