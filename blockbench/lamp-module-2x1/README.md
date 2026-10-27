# Módulo de lâmpada 2×1 (v1, aguardando redesign)

Identidade compartilhada: [guia visual](../../docs/visual-design-guide.md), seção 9.

Esta variante ainda é v1: alonga em duas vezes, no eixo local X e em torno do centro, a
lâmpada 1×1 **v1**, cuja fonte agora fica em `base-v1/` (`lamp_module_1x1_v1*.bbmodel`). A
lâmpada 1×1 em uso no jogo já é a v2.1 (tubo nixie, `../lamp-module-1x1/`), então as duas
lâmpadas estão visualmente diferentes até este módulo ser redesenhado. No redesign, o 2×1
deixa de ser um esticamento (guia, seção 9.5).

- `lamp_module_2x1_off.bbmodel` e `lamp_module_2x1_on.bbmodel`: fontes derivadas.
- `base-v1/`: fontes e export da 1×1 v1, mantidos só para gerar este módulo.
- Texturas v1 em runtime: `lamp_housing`, `lamp_lens_red`, `lamp_emitter_off/on`.

A integração gira o modelo 90° quando o footprint ocupa 1 coluna × 2 linhas. Para recriar
os `.bbmodel` derivados e os dois modelos JSON do jogo:

```powershell
node .\blockbench\lamp-module-2x1\create-models.mjs
```
