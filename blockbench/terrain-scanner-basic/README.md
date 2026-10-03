# Terrain Scanner Basic — fonte visual

Fonte editável: `terrain_scanner_basic.bbmodel`, Java Block/Item para Minecraft
1.21.1, cuboides sem rotações, materiais 32×32 incorporados. Segue o
[guia de identidade visual](../../docs/visual-design-guide.md).

A base foi preparada localmente para importação. Pelo Computer Use no
Blockbench, a antena recebeu um rebaixo de 0,25 unidade em relação à moldura e
os nove elementos do padrão frontal receberam realces de latão pintados.

## Identidade e orientação

- Carcaça de metal claro, moldura de grafite, montagem em relevo e conector traseiro.
- Frente canônica **NORTH / Z negativo**. A antena é o painel frontal rebaixado,
  não o conector de dados nem o emblema na tampa.
- Setas amarelas nas laterais e na tampa apontam para a frente. A face EAST usa
  UV espelhado horizontalmente para manter a seta no sentido correto.
- Padrão frontal de nove elementos em cerâmica/latão: identidade gráfica de
  antena, não lâmpadas, botões ou uma representação exata da distribuição dos casts.
- O centro da face é X/Y=8. Sua superfície fica em Z=0,25; a origem existente dos
  casts permanece imediatamente fora da face NORTH da célula, em Z≈−0,016.
- Modelo opaco, sem emissão, transparência, LEDs de estado ou animação.
- As quatro orientações horizontais existentes giram modelo e colisão juntos.
  Não foram adicionadas orientações verticais nem alterada a lógica dos scans.
- A versão Advanced não é modificada nesta entrega.

## Materiais compartilhados

Metal, grafite, latão, emblema azul e conector reutilizam as texturas do Radar
Altimeter. O projeto incorpora cópias dessas fontes para permitir edição no
Blockbench, mas o exportador confere sua igualdade com os assets compartilhados
e **não os sobrescreve**. Um ajuste global de material deve ser tratado como
mudança explícita da família, não como efeito colateral do export deste modelo.

Três materiais são próprios: `terrain_scanner_case`, `terrain_scanner_antenna`
e `terrain_scanner_top`.

## Exportação e validação

Após salvar o `.bbmodel` com texturas incorporadas, execute na raiz do mod:

```powershell
node blockbench/terrain-scanner-basic/export-assets.mjs
```

O exportador preserva edições nativas, converte UVs 32×32 para 0–16 e atualiza
`models/block/terrain_scanner.json` e os três PNGs próprios. O modelo de item
herda o modelo de bloco. Não reexecute `create-base-model.mjs` sobre a fonte
refinada; esse script é somente o bootstrap inicial.

Atualizações de geometria precisam manter as formas de `TerrainScannerBlock`
coerentes. Os testes comparam modelo/seleção/colisão em NORTH, EAST, SOUTH e WEST,
validam a frente, rotações de blockstate, UVs e texturas opacas. Colocação,
aparência no inventário e leitura visual durante movimento exigem validação
manual no jogo; build não substitui esse aceite.
