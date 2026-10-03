# Terrain Scanner Advanced — fonte visual

Fonte editável: `terrain_scanner_advanced.bbmodel`, Java Block/Item para
Minecraft 1.21.1, com texturas **32×32 incorporadas**.

O projeto começa como cópia do `.bbmodel` Basic já refinado, não como uma nova
carcaça. Os 13 cuboides, recuos, pés, emblema e socket traseiro são idênticos.
`duplicate-basic.mjs` registra essa origem e recusa sobrescrever uma fonte
existente. Não reexecute o bootstrap sobre o modelo editado.

## Diferenciação

- Antena frontal com **5×5 elementos**: 25 quadradinhos de quatro texels por lado,
  separados por dois texels. Não aumenta a resolução nem a geometria.
- Os centros receberam latão claro `#E7C78D` usando Paint Bucket/Colors na
  interface nativa do Blockbench; bordas e realces preservam a família do Basic.
- Faixa dupla azul dessaturada `#31657E` perto da traseira nas laterais, tampa e
  face inferior. Na traseira, duas marcas reaparecem ao lado do socket.
- Setas amarelas preservadas. Marcas azuis são pintura, não LEDs/estado.
- Frente canônica **NORTH**, painel em Z=0,25 e socket traseiro em Z=16. Modelo
  opaco, sem emissão, transparência, animação ou alterações nos casts.

## Materiais e exportação

As cinco texturas compartilhadas do Radar Altimeter são somente leitura. As
seis texturas próprias usam prefixo `terrain_scanner_advanced_`. Basic e seus
assets não são sobrescritos. Não é necessário salvar PNGs separados no editor:
o `.bbmodel` incorpora as texturas e o exportador gera os PNGs de runtime.

Após salvar no Blockbench, na raiz do mod:

```powershell
node blockbench/terrain-scanner-advanced/export-assets.mjs
```

O exportador verifica a geometria contra o Basic, dimensões das texturas e
integridade dos materiais compartilhados. Preserva UVs e edições nativas.

Seleção, colisão e encaixe adaptativo do Avionics Cable compartilham a geometria
do Basic nas quatro orientações. API, alcance, abertura configurável, GUI e
distribuição de raios do Advanced permanecem inalterados; os 25 quadradinhos
não representam literalmente 25 raios.

Testes cobrem equivalência geométrica, faces/rotações, sockets, 25 elementos
separados, marcas azuis, texturas opacas e UVs. Verificação no jogo permanece
necessária: comparar Basic/Advanced, checar inventário, colocação, cabos em todas
as faces e acesso à tela de configuração.
