# RAG Retrieval-Layer Evaluation

- Evaluation type: synthetic, chunk-grounded retrieval evaluation
- Source: `SoCS MSc Project Information 25-26.pdf` (SHA-256 `f94e91e909e6c85f7eeb5b393f7030887439ce781ee23cc82367e7eb0278ce31`)
- Indexed chunks: 51
- Queries: 102 (51 direct, 51 template-based reformulations; two per chunk)
- Chunking: 500 characters with 50 character overlap
- Embeddings: `text-embedding-v3`, dimension 1024
- Retrieval path: application `KnowledgeService.search()` -> `RedisVectorStore.search()`; top-3 KNN over `knowledge_public`

## Aggregate metrics

| Metric | All queries | Direct | Paraphrased |
|---|---:|---:|---:|
| Recall@1 | 97.1% | 98.0% | 96.1% |
| Recall@3 | 100.0% | 100.0% | 100.0% |
| MRR | 0.9853 | 0.9902 | 0.9804 |

## Interpretation boundary

The queries and relevance labels were generated from the indexed chunks. The reformulation set uses a deterministic template that retains salient source terms; it is not a set of independent human-written queries. This is a synthetic, chunk-grounded evaluation of retrieval ranking for this document and configuration. It does not establish final answer correctness, citation correctness, user satisfaction, robustness to unseen user language, or production-scale performance. Recall and MRR measure whether the labelled source chunk was returned and how highly it was ranked; they do not evaluate the language model response.

## Reproducibility

The PDF was uploaded through the admin knowledge-document endpoint, indexed asynchronously through the production parser/indexing path, and queried through the `KnowledgeService.search()` bean in a temporary Spring context. The temporary MongoDB and Redis Stack instances used host ports 27019 and 16382; production services were not changed.
