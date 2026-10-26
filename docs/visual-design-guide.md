# Guia visual v2 — linguagem Create / Create Aeronautics

> Substitui a direção "aviónica industrial" da v1 (2026-10). A v1 continua no
> histórico do git. As regras técnicas que seguem válidas estão na seção 11.
> Ferramenta oficial de modelagem e pintura: **Blockbench**.
>
> **Leitura obrigatória antes de qualquer pedido de modelagem ou texturização**
> neste mod (bloco, item, módulo, variante ou ajuste de modelo existente).

## 0. Por que mudar

Os modelos atuais são bem acabados, mas parecem de outro mod. Uma análise de
1.294 modelos de bloco do Create 6.0.10, 335 do Create Aeronautics (Simulated)
1.3.2 e 77 do Aeronautics 1.3.2, comparados com os nossos, mostra onde está a
diferença:

| Critério | Create / Simulated / Aeronautics | Nossos modelos atuais |
| --- | --- | --- |
| Densidade de textura | **1 texel por unidade** em 94–98% das faces (16 px por bloco) | Mista: 2–3 texels/unidade em faces grandes, até 32 texels numa face de 0,5 (textura inteira esticada em toda face) |
| Coordenadas | Unidades inteiras; menor dimensão típica 2, às vezes 1 | Frações de 0,25/0,5/0,75 em quase todos os modelos |
| Complexidade | Mediana de 2–5 elementos, p90 8–15 | Sensores e módulos: 13–43 elementos (lâmpada e cabo: 3–6) |
| Paleta | Simulated reutiliza **cores exatas do Create** (altitude sensor 92%, gimbal 95%, navigation table 90%); os bearings do Aeronautics, 95% | 0–7% (altímetro: 2%) |
| Origem das texturas | Pintadas à mão; ~29–55% dos modelos trazem `"credit": "Made with Blockbench"` | Geradas por script com funções de ruído e refinadas depois |
| Temperatura | Quente: latão, madeira, cobre, andesito esverdeado | Fria: prata, azul-petróleo, amarelo de sinalização |

O altímetro atual parece um equipamento real de aviação. Um bloco do Create
parece uma **máquina montada com as peças do próprio Create**.

Pranchas de referência (locais, fora do git): `reference/visual/`.

![Nossos sensores vs. análogos do Create e do Simulated](../reference/visual/compare_ours_vs_create.png)

## 1. Princípio

**Todo bloco deve parecer algo que a equipe do Create poderia ter feito.**

1. **Feito de materiais do Create.** Carcaça de andesito, latão ou cobre,
   madeira, ferro industrial. Nada de materiais novos inventados.
2. **Função mecânica à vista.** Mostrador, lente, ponteiro, hélice ou antena
   grande o bastante para ler a 5–10 blocos. Esconder a "eletrônica".
3. **Robusto e em camadas.** Base, corpo e tampa bem separados por volume e
   material, nunca uma caixa lisa com adesivos.
4. **Quente e saturado na medida.** Rampas com variação de matiz, contorno
   escuro e luz forte no topo, como as texturas do Create.
5. **Cor com significado.** Vermelho redstone é estado de saída. Gemas azul
   e vermelha indicam frequência. Pergaminho é leitura. Ver a seção 6.

## 2. Contexto de uso (builds da comunidade)

Fontes: galeria oficial do Create Aeronautics no Modrinth (Cloud Skipper,
Airships, Blue Plane) e cerca de 47 schematics do createmod.com (dirigíveis,
zeppelins, aviões, biplanos, helicópteros).

- **Dirigíveis:** cascos de madeira escura (spruce/dark oak), envelopes de
  lona branca ou colorida com cintas de madeira clara/latão, maquinário do
  Create exposto (cobre, casings de andesito/latão, engrenagens), propellers
  de vela.
- **Aviões e biplanos:** fuselagem de madeira, asas de vela/lona, pintura de
  cores fortes (vermelho/amarelo, ciano/verde-água, xadrez), cockpit de vidro
  com moldura escura.
- **Helicópteros e builds "modernos":** mais ferro industrial, metal escuro e
  concreto, mas o maquinário continua sendo do Create.

O que isso significa para os sensores:
- Eles vivem **embutidos no casco**: o altímetro na barriga, o scanner no nariz.
  São vistos de 3–10 blocos, ao lado de casings do Create.
- Precisam combinar tanto com madeira quanto com metal. Por isso a carcaça
  segue as famílias de casing do Create (andesito/latão/cobre).
- Silhueta e face funcional precisam ler de longe. Detalhe fino some.

## 3. Geometria (Blockbench)

| Regra | Valor |
| --- | --- |
| Grade | Snap de **1 unidade** (16 = 1 bloco). Frações só com motivo forte, como o recuo de 0,5 de uma lente, e nunca para "afinar" uma peça |
| Elemento mínimo | 1 unidade; preferir 2 ou mais em peças visíveis |
| Elementos por bloco | Simples: 2–8. Instrumento: até ~15. Acima de 20, justificar (Create chega a 25, Simulated a 39) |
| Rotação | Só 22,5° e 45°, e só em peças mecânicas (ponteiro, alavanca, aleta). Carcaça sempre alinhada à grade |
| Camadas | Base/pé (2–3 de altura) + corpo + tampa ou placa (2 de altura). Corpo costuma recuar 1 unidade em relação à tampa ou base |
| Peça funcional | Grande e saliente: lente 6–10 unidades, mostrador com moldura, antena com volume. É o item que define a silhueta |
| Partes móveis | Modeladas como elemento ou modelo separado (como as partials do Simulated: `hand`, `needle`, `fan`), para animar no futuro sem refazer a carcaça |

### Vocabulário formal recorrente

- **Moldura de casing:** borda de 2 px de metal ao redor de um miolo de madeira
  ou pedra (andesite/brass/copper casing).
- **Placa de topo com quadrados concêntricos:** content observer, threshold
  switch, laser sensor, optical sensor, navigation table.
- **Faixa de redstone:** fenda vermelha na lateral (stockpile/threshold switch)
  ou nos cantos (laser sensor), escura sem sinal e acesa com sinal.
- **Cantoneiras e pés:** pequenos blocos nos cantos (navigation table, docking
  connector).
- **Base de andesito com tábua de madeira:** altitude sensor, physics assembler.
- **Bocal escalonado:** laser pointer, anéis sucessivos de 1–2 unidades.

## 4. Texturas (Blockbench Paint)

### Densidade: 1 texel = 1 unidade, sempre

- Material isolado: PNG **16×16**.
- Peça com várias faces diferentes: **atlas 32×32** (64×64 só para modelos
  grandes), em que **cada face ocupa no atlas o tamanho real dela em pixels**.
  Uma face de 14×11 unidades usa 14×11 px.
- No Blockbench (formato Java Block): **um espaço de UV único para todas as
  texturas**, igual à resolução do projeto. O Blockbench 5.2 ignora o UV size de
  cada textura ao exportar Java e usa a resolução do projeto. Com projeto 32×32,
  um atlas de 32 px fica 1:1 com Auto UV, mas uma textura de 16 px precisa de UVs
  dobradas (uma face de 8 unidades usa 16 de UV). Conferir o export com o
  exportador do bloco, que falha se a densidade não for 1:1. Assim cada face recebe UV na
  mesma escala da geometria. **Nunca** mapear `[0,0,32,32]` em todas as faces,
  que é o que os nossos `.bbmodel` atuais fazem. O `.bbmodel` do velocity
  sensor do Simulated usa a convenção equivalente: UV 16 com atlas 32, de modo
  que uma face de 16 unidades usa 8 de UV, ou seja, 16 px.
- Conferir com a grade de pixels do Blockbench ligada: os texels de faces
  vizinhas precisam ter o mesmo tamanho na tela.

### Pintura

- **Pintar à mão** no modo Paint do Blockbench, com a paleta da seção 5
  carregada (`blockbench/_palette/create-aeronautics.gpl`, ou `.hex` se a versão
  do Blockbench não importar `.gpl`; importar pelo menu do painel de cores). Scripts servem
  para criar a geometria inicial, não para gerar texturas com ruído.
- **Poucas cores:** 7–20 por 16×16. Cada material usa uma rampa de 5–8 tons.
- **Luz:** topo e esquerda mais claros, base e direita mais escuras. Aresta
  externa com 1 px do tom mais escuro da rampa; aresta interna da moldura com
  1 px do tom mais claro.
- **Variação de matiz:** no latão, a sombra puxa para marrom-vinho (`#592424`)
  e a luz para amarelo claro (`#FFEB8C`). A madeira sombreia para marrom quente,
  nunca para cinza.
- **Textura de material:** veio de madeira em linhas de 1 px, tábuas separadas
  por uma linha escura, rebites de 1 px nos cantos das molduras. Nada de ruído
  aleatório.
- **Assets do Create são *All Rights Reserved*** (só o código é MIT). Podemos
  **referenciar** as texturas pelo id, porque elas são carregadas do jar do
  Create em runtime. Não podemos **copiar** PNGs, pixels ou trechos deles para o
  nosso repositório, nem embuti-los no `.bbmodel`. Para ver a textura no
  Blockbench, usar uma cópia local em `reference/create-textures/` (fora do git)
  via `relative_path`. Paleta (HEX) e estilo podem ser seguidos livremente.
- **Reutilizar antes de pintar.** Quando a peça é literalmente um material do
  Create, referenciar a textura dele no modelo, como o Simulated e o
  Aeronautics fazem: `create:block/andesite_casing`, `create:block/brass_casing`,
  `create:block/industrial_iron_block`, `create:block/axis`. O Create é
  dependência obrigatória do mod.
- Sem antialiasing, sem gradiente suave, sem texto legível (letras viram ruído
  a 16 px por bloco).

## 5. Paleta (HEX extraídos dos jars)

Use as rampas como estão. Ao estender, interpolar dentro da mesma rampa.
Arquivo para o Blockbench: `blockbench/_palette/create-aeronautics.gpl`.

| Material | Rampa (escuro → claro) | Onde aparece |
| --- | --- | --- |
| Latão | `#592424` `#724731` `#9E6947` `#BF8E55` `#CEA05A` `#E4B763` `#F7CB6C` `#FFEB8C` | brass casing/block, molduras "smart", optical sensor |
| Madeira escura | `#301E0E` `#3A2411` `#442B15` `#4F3118` `#53381A` `#5A3E1E` | miolo do brass casing, corpo dos instrumentos de latão |
| Madeira média | `#553A1F` `#5A4424` `#614B2E` `#70522E` `#7A5A34` `#82613A` `#886539` | miolo do andesite casing, laser sensor, velocity sensor |
| Andesito | `#505351` `#60635F` `#686C68` `#707570` `#828784` `#8F9691` `#9AA49D` `#A8B3AB` | andesite casing, altitude sensor (cinza levemente esverdeado) |
| Cobre | `#904931` `#9A5038` `#A75A40` `#B26247` `#C26B4C` `#C87456` `#D67B5B` `#E3826C` | copper casing, steam vent, potato cannon |
| Ferro industrial | `#353438` `#3F3E42` `#444444` `#494848` `#4F4F4F` `#595858` `#676161` `#757171` | bearings do Aeronautics, peças pesadas |
| Metal escuro | `#2B2B2B` `#353535` `#3B3B3C` `#464647` `#4A4A4B` `#525255` | aros, bocais, molduras escuras |
| Pergaminho | `#676562` `#949188` (marcas) + `#D4C2A4` `#E0D2AE` `#EEE4CE` `#F6EDD8` `#F9F2DE` | mostradores (altitude sensor radial/linear) |
| Redstone desligado | `#460000` `#500101` `#580101` | indicador sem sinal |
| Redstone ligado | `#970000` `#AE0000` `#CD0000` (+ brilho `#FD0000`) | indicador com sinal |
| Lente escura | `#13161D` `#171B23` `#20232A` `#282C37` `#333846` `#464C5F` | lente do laser sensor, vidros ópticos |
| Gema azul | `#5059AB` `#5371C6` | gimbal, receivers (par com a gema vermelha) |
| Lona | `#868480` `#9B9892` `#B6AEA1` `#CEC4B5` `#E4E4DF` `#F1F1F0` | envelopes, velas |

**Sai da paleta:** azul-petróleo de identidade (`#31657E`) e amarelo de
sinalização (`#E4A92F`), que não aparecem em nenhuma textura do Create ou do
Aeronautics. Também sai a prata fria (`#BABDBF` e vizinhas) como cor dominante
de carcaça: ela existe pontualmente no Create, mas nunca como material principal.

## 6. Cor com função

| Sinal visual | Significado no Create/Aeronautics | Uso nos nossos sensores |
| --- | --- | --- |
| Faixa ou ponto **vermelho redstone** | Saída de redstone; escuro sem sinal, aceso com sinal (o modelo troca para `block_powered`) | Todos os nossos sensores emitem redstone. O indicador deve acender com potência > 0 (exige estado `powered` no bloco; decidir na implementação) |
| **Gemas azul + vermelha** lado a lado | Par de frequência do Redstone Link | Opcional, só onde o jogador configura frequências (os sensores aceitam Link) |
| **Pergaminho** com marcas cinza | Leitura/medição | Mostrador de altura ou distância, se houver |
| **Lente escura** com reflexo | Emissor/receptor óptico | Face de medição (raycast) dos scanners e do altímetro |
| **Latão** | Tier "smart"/avançado | Variante avançada (Advanced Terrain Scanner) |

## 7. Famílias de material por tier (proposta)

Segue a lógica do Create: andesito é o básico, latão é o avançado ou "smart",
cobre é fluidos e vapor, ferro industrial é a parte pesada do Aeronautics.

| Bloco | Família proposta | Referências diretas |
| --- | --- | --- |
| Radar Altimeter | **Latão + madeira escura** (decisão de 2026-10-21), lente para baixo | optical sensor, stockpile/threshold switch, content observer, laser pointer |
| Terrain Scanner | **Andesito + madeira média** (v2 entregue), bocal escalonado frontal | andesite casing, laser sensor, laser pointer |
| Advanced Terrain Scanner | **Latão + ferro industrial** (v2 entregue), barra com três lentes | content observer, smart observer, optical sensor |
| GPWS Computer (experimental) | Latão + madeira escura | navigation table, stockpile switch |
| Avionics Cable (experimental) | Decidir no redesign | rope, belts, fluid pipe |

### Diferenciação entre os sensores

Regra: cada sensor precisa ser reconhecível **num relance**, inclusive reduzido a ~76 px
(um bloco visto a uns 10 blocos de distância). Use pelo menos quatro pistas
independentes, e não dependa de uma só:

| Pista | Radar Altimeter | Terrain Scanner | Advanced Terrain Scanner |
| --- | --- | --- | --- |
| Tier e material (cor dominante) | Latão + madeira escura (marrom-dourado) | Andesito + madeira média (cinza e marrom claro) | Latão + ferro industrial (dourado sobre grafite) |
| Face de medição | Embaixo | Frente | Frente |
| Peça óptica (silhueta) | Lente quadrada rente, num aro | Bocal escalonado com lente pequena | Barra larga com três lentes em arco |
| Fenda de redstone | Horizontal, nas 4 laterais | Vertical, nas laterais | Horizontal, só nas faces que emitem |
| Topo | `create:block/brass_casing` | Tampa de andesito com tábuas | Placa de latão concêntrica |

Novos sensores devem escolher combinações que não colidam com estas. Variantes de um
mesmo bloco (básico e avançado) mudam o tier de material e a peça óptica, e mantêm o
volume do corpo e os sockets.

## 8. Radar Altimeter (v2 entregue em 2026-10-21)

Resultado: [README do bloco](../blockbench/radar-altimeter/README.md). Três cubos
(corpo, aro e lente), atlas 32×32 + lente 16×16 em 1:1, topo com
`create:block/brass_casing` e variante `powered` pronta (falta o estado no bloco).
O brief original fica abaixo como registro.

Requisitos fixos do código:
- Mede **para baixo**. O centro inferior da antena continua em `(8, 0, 8)`,
  a origem dos raycasts.
- Ocupa 0–16. A colisão em `RadarAltimeterBlock` acompanha a nova geometria.
- Emite redstone pelas faces e via Link.

Direção visual (decidido: **linhas e tons dos blocos de latão**):
- Corpo na família do **optical sensor / stockpile switch**: miolo de madeira
  escura emoldurado em latão e placa de topo de latão com o motivo concêntrico.
- Face inferior com a peça funcional grande: lente escura escalonada, como
  no laser pointer virado para baixo, ou prato de antena em cobre. A lente lê
  melhor como "raio para baixo".
- Uma fenda de redstone lateral que acende com sinal.
- Nada de setas pintadas. A direção vem da lente embaixo e da silhueta.
- Diferenciar do **altitude sensor** do Simulated, que mede altitude e tem
  mostrador vertical. O nosso mede distância até o chão, e a leitura é a lente
  apontada para baixo, não um mostrador.

## 9. Fluxo no Blockbench

1. **Novo projeto:** *Java Block/Item*, nome = id do bloco, resolução 16 (ou
   32 se for atlas), *Box UV* desligado.
2. **Referência lado a lado:** importar no mesmo workspace o JSON do modelo
   análogo do Create ou do Simulated, extraído do jar, para comparar escala e
   densidade. Não exportar a referência.
3. **Geometria:** snap de 1 unidade e camadas (seção 3), sem textura. Validar
   a silhueta em cinza.
4. **Texturas:** criar o PNG ou atlas, *UV Size* = tamanho do PNG e Auto UV.
   Pintar com a paleta `.gpl`. Reaproveitar `create:block/...` onde couber.
5. **Variantes:** `block` e `block_powered` só trocam a textura do indicador,
   nunca a geometria.
6. **Exportar:** *File → Export → Java Block/Item Model* para
   `src/main/resources/.../models/block/`. Os PNGs vão para `textures/block/`.
   Manter o `.bbmodel`, com texturas incorporadas, em `blockbench/<bloco>/`.
7. **Comparar no jogo:** colocar o bloco entre casings de andesito/latão e num
   casco de madeira, de dia e de noite, com e sem shader.

### Checklist de entrega

- [ ] Densidade 1:1 em todas as faces (pixel grid uniforme)
- [ ] Coordenadas inteiras (ou fração justificada no README do bloco)
- [ ] Elementos dentro do orçamento da seção 3
- [ ] ≥ 90% dos pixels com cores da paleta (seção 5) ou texturas do Create
- [ ] Silhueta em camadas e peça funcional legível a 8 blocos
- [ ] Face funcional coerente com a direção real do código
- [ ] Indicador de redstone escuro/aceso, se houver
- [ ] Ao lado de casings do Create, parece da mesma família
- [ ] `.bbmodel` atualizado, exportação e testes de assets verdes

## 10. Fontes

- Jars locais: `create-1.21.1-6.0.10.jar` e
  `create-aeronautics-bundled-1.21.1-1.3.2.jar` (aeronautics, simulated,
  offroad).
- [Galeria do Create Aeronautics no Modrinth](https://modrinth.com/mod/create-aeronautics/gallery)
- [Create Aeronautics no CurseForge](https://www.curseforge.com/minecraft/mc-mods/create-aeronautics)
- [Busca "aeronautics" no createmod.com](https://createmod.com/search?q=aeronautics),
  mais as buscas airship, zeppelin, aeronautics plane, aeronautics helicopter e biplane.
- [Create Aeronautics (Simulated) no createmod.com](https://createmod.com/de/mods/simulated)

## 11. Regras técnicas mantidas da v1

Estas continuam valendo, independentemente da estética:

- **Módulos da mesa:** modelar com a mesa voltada para cima, centro em
  X/Z = 8 e apoio em Y = 0. O renderer posiciona e rotaciona; não embutir
  offsets no modelo.
- **Lâmpadas:** lente, base e núcleo são materiais separados. Aceso e apagado
  mantêm a mesma geometria. A emissão pertence à lâmpada, não à mesa.
- **Face funcional inequívoca** e coerente com a direção do código, inclusive
  em sublevels rotacionados.
- **Sem dependência de shader:** a leitura é sem shader. Testar também com
  shaders (os glitches das lâmpadas com Complementary seguem em aberto, ver
  LÂMPADA-01).
- Sem faces coplanares sobrepostas e sem cascas duplicadas. Colisão acompanha
  a geometria relevante.
- O `.bbmodel` é a fonte. O JSON/PNG em `src/main/resources` é o asset de
  runtime e precisa ficar coerente com ele.
