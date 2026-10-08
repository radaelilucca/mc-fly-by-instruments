# Módulo FMS — 2 de largura × 3 de altura

Fonte editável: [fms_module_2x3.bbmodel](fms_module_2x3.bbmodel).
Identidade compartilhada: [guia visual](../../docs/visual-design-guide.md).

Base preparada em arquivo e refinada/texturizada no Blockbench via Computer Use.
O display é apenas uma superfície preta opaca, sem conteúdo nem emissão, reservada
para uma implementação posterior. Sua área, moldura e teclas laterais ocupam os
dois terços superiores; o teclado compacto ocupa o terço inferior.

Carcaça grafite, moldura prateada fria, parafusos estilizados e teclas quase pretas
seguem a paleta dos sensores. As duas teclas de ação e uma pequena marca na
carcaça usam azul dessaturado. Não há marcas de fabricantes, texto minúsculo
obrigatório, transparência ou dependência de shaders.

- Java Block/Item, 43 cuboides, atlas opaco 64×64 incorporado e PNG local.
- Centro em X/Z=8; apoio em Y=0, sem embutir a altura da mesa.
- Dimensões visuais: 9,5 × 14,5 unidades no plano, altura máxima 2,55.
- Footprint: 2 colunas × 3 linhas, com respiro até os limites das seis células.
- O renderer usa quads crus, sem transformações de item, e o mesmo posicionamento
  dos outros módulos. Mesas de parede/teto seguem a orientação da mesa.
- As transformações de hotbar/inventário e mãos ficam em
  `models/item/fms_module.json`: a GUI mostra a face quase de frente, e as mãos
  usam escala reduzida com o display inclinado em direção ao jogador.
- Saves legados 2×2 que não puderam ser ampliados usam a mesma peça com a
  profundidade reduzida para caber no footprint preservado.

Depois de salvar alterações no Blockbench:

```powershell
node .\blockbench\fms-module-2x3\export-assets.mjs
```

O exportador conserva as alterações do editor e verifica centro, apoio, dimensões
e cubos não degenerados. `create-base-model.mjs` serve só para gerar uma base
nova; executá-lo novamente substitui o acabamento salvo no `.bbmodel`.

Inspeção feita no editor; posicionamento e aparência finais precisam de
validação in-game. A integração mantém os fluxos existentes do FMS.
