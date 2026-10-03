# Módulo de buzzer 1×1

Identidade compartilhada: [guia de estilo e paleta](../../docs/visual-design-guide.md).

Projeto Java Block/Item para Minecraft 1.21.1, com texturas incorporadas no
[`buzzer_module_1x1.bbmodel`](buzzer_module_1x1.bbmodel). A geometria e os materiais
iniciais foram preparados para importação, acabamento e revisão visual no Blockbench
usando Computer Use.

No editor foram refinados os reflexos da lateral, os poros da tampa e a escala
final (95% em torno do pivô 8,0,8), preservando o apoio em Y=0 e o centro em X/Z=8.

O módulo tem diâmetro de aproximadamente 2,85 pixels e altura de 1,9 pixels, contra
3,4 × 3,4 pixels da lâmpada 1×1. A origem de montagem é Y=0; X/Z são centralizados
em 8. O renderer o posiciona no centro da célula e na superfície da mesa usando
o mesmo cálculo das lâmpadas, inclusive para mesas no teto e nas paredes.

São 16 facetas externas, tampa e base, 16 faces internas do furo e seu fundo
rebaixado: 35 faces exportadas. As rotações usam apenas incrementos de 22,5°,
até ±45°, suportados pelo modelo Java 1.21.1. Não há meshes nem faces coplanares
sobrepostas. As texturas de 64×64 usam alpha binário, com renderização cutout.

- `buzzer_housing`: preto piano, com reflexos pintados discretos e bordas escuras.
- `buzzer_top`: preto poroso, borda fina e abertura central recortada.
- `buzzer_base`: apoio preto opaco, sem abertura central.
- `buzzer_hole`: interior e fundo do pequeno rebaixo, preto profundo.

Depois de salvar alterações pelo Blockbench, reexporte os assets ativos:

```powershell
node .\blockbench\buzzer-module-1x1\export-assets.mjs
```

`create-base-model.mjs` é apenas o preparo inicial do projeto; executá-lo novamente
substitui a fonte `.bbmodel`. Para manter o acabamento salvo no editor, utilize
somente o exportador. Esta entrega cobre o visual, instalação física, remoção,
persistência e descoberta do módulo pela API da mesa. O buzzer ainda não emite som.
