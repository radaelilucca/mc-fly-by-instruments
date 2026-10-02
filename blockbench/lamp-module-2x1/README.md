# Módulo de lâmpada 2×1

Esta variante alonga o modelo 1×1 em duas vezes no eixo local X, em torno do centro. Mantém a altura, profundidade, UVs e texturas originais; por isso, o acabamento, o estado aceso/apagado e o material visual são compartilhados com a lâmpada 1×1. A integração gira o modelo 90° quando o footprint ocupa 1 coluna × 2 linhas.

- `lamp_module_2x1_off.bbmodel` e `lamp_module_2x1_on.bbmodel`: fontes Java Block editáveis no Blockbench, com texturas incorporadas.
- Modelos JSON instalados em `src/main/resources/assets/aeronautics_fly_by_instruments/models/block/`.
- Texturas reutilizadas da pasta `lamp-module-1x1`; não há cópias divergentes.

Para recriar a variante após editar as fontes 1×1 e sincronizar os modelos do jogo, execute:

```powershell
node .\blockbench\lamp-module-2x1\create-models.mjs
node .\blockbench\lamp-module-1x1\export-assets.mjs
```

O primeiro script gera os `.bbmodel` derivados, os JSON exportados e os dois modelos JSON usados pelo jogo. O exportador então valida que os estados compartilham geometria e que o 2×1 é exatamente o alongamento do 1×1 no eixo local X; ele também atualiza as texturas exportadas.
