# Guia de identidade visual — Aeronautics: Fly by Instruments

## 1. Direção artística

**Aviónica industrial em voxel, com leitura arcade-first.** Instrumentos de
aviação reais inspiram materiais, organização e função; o resultado precisa
parecer um componente construível dentro de Minecraft, não uma miniatura
fotorealista de equipamento aeronáutico.

O jogador deve reconhecer primeiro a função e a orientação, depois apreciar os
detalhes. O acabamento deve combinar com uma contraption de Create sem depender
de copiar seus assets, transformar tudo em engrenagens ou exigir conhecimentos
de cockpit real.

Este guia consolida os assets existentes e estabelece diretrizes para os próximos
modelos. Não altera modelos, funcionalidades, receitas ou semântica dos canais.
As propostas de cores futuras são explicitamente separadas do que já existe.

### Hierarquia de leitura

1. **Silhueta:** distinguir sensor, lâmpada, buzzer e equipamento de controle.
2. **Face funcional:** identificar onde o dispositivo mede, indica ou emite som.
3. **Material:** reconhecer metal, vidro, plástico e superfície de antena.
4. **Detalhe:** juntas, parafusos, conectores e pequenos emblemas.

Um detalhe que prejudica os três primeiros níveis deve ser simplificado.

## 2. Paleta e papel das cores

As referências abaixo vêm das texturas atuais. Materiais com variação de pixels
possuem uma família de tons; o HEX indicado é uma âncora, não uma exigência de
substituir todos os pixels por uma cor uniforme.

| Material/papel | Âncoras atuais | Uso e intenção |
| --- | --- | --- |
| Metal claro | `#BABDBF`, `#9DA1A3` | Carcaças, tampas e montagem. Prata levemente fria; industrial sem branco puro. |
| Aresta iluminada do metal | `#E0E3E5`, `#D1D5D7` | Pequenos realces pintados, não contornos brancos em todas as peças. |
| Grafite estrutural | `#343B45` | Base das lâmpadas; separa os módulos da superfície da mesa. |
| Moldura escura | `#2F3437`, `#181D20` | Juntas, isolamento e moldura do altímetro. Contraste de construção, não estado ligado/desligado. |
| Preto do buzzer | `#0C0D0E`, `#232527` | Corpo preto piano com realces contidos. Usar quase-preto para preservar volume. |
| Preto poroso/interior | `#1A1A1A`–`#2A2A2A`, `#040405` | Tampa texturizada e cavidade profunda. O furo é mais escuro que a tampa. |
| Latão | `#B1853E`, `#E7BB67` | Conectores e detalhes pequenos. Evoca hardware, não joalheria nem ouro dominante. |
| Marca de direção | `#E4A92F` | Setas de montagem/funcionamento; contraste quente sobre carcaça neutra. |
| Cerâmica da antena | `#D8D5CA`, `#B9822F` | Radome claro com padrão em latão, distinto da tampa metálica. |
| Azul de identidade | `#31657E`, `#1E3A49`, `#CFE3E3` | Emblemas e identificação técnica decorativa; não significa automaticamente conexão ou leitura válida. |
| Detalhe central da antena | `#70A6B2` | Pequeno acento dessaturado. Não é emissivo nem um indicador de estado. |

### Lâmpadas: família própria dentro da mesma identidade

| Parte | Referência atual | Regra |
| --- | --- | --- |
| Base | `#343B45`, alpha 255 nos texels usados | Apoio opaco, sem brilho emissivo. |
| Lente vermelha | `#D41D38`, alpha 156/255 | Vidro colorido uniforme; evitar manchas rosas ou reflexos pintados assimétricos. |
| Núcleo apagado | `#343B45`, alpha 255 nos texels usados | Deve permanecer visivelmente apagado mesmo quando a lente conserva sua cor. |
| Núcleo aceso | `#FFE4DC`, alpha 255 nos texels usados | Luz quente filtrada pela lente, com intensidade visual independente por módulo. |

A lente, a base e o núcleo são materiais separados. Novas cores de lâmpada devem
preservar geometria, alturas, UVs e acabamento da família; não redesenhar a
carcaça para cada cor. O vermelho é a variante atual, não uma restrição que
reserve toda lâmpada vermelha exclusivamente a um canal específico do GPWS.

### Cores futuras — proposta, ainda não implementada

Para ampliar a família, partir de âmbar, verde e azul/ciano com saturação
compatível com a lente vermelha. HEXs, alpha e núcleos dessas variantes precisam
de aprovação visual no jogo antes de virarem referências definitivas.

Quando houver presets de alertas, vermelho pode sugerir urgência, âmbar atenção
e verde estado normal. Isso é uma convenção sugerida para presets, **não um novo
vínculo obrigatório entre cor, módulo e canal**. Forma, estado aceso/apagado e
configuração também comunicam a função; cor não deve ser a única pista.

Como orientação compositiva, manter carcaças majoritariamente neutras e reservar
cores saturadas para pequenas marcas ou superfícies funcionais. Uma lâmpada é
a exceção natural: sua lente colorida é a própria superfície funcional.

## 3. Materiais e acabamento

### Metal pintado ou escovado

- Planos grandes e simples, juntas escuras e um realce discreto nas arestas.
- Rivetes/parafusos estilizados como poucos pixels ou pequenos cuboides apenas
  quando o relevo realmente muda a silhueta.
- Variação tonal de baixo contraste; evitar ruído que pareça pedra ou sujeira.
- Carcaças não recebem emissão, transparência ou brilho de estado por padrão.

### Plástico preto piano

- Corpo quase-preto, com reflexos pintados curtos e pouco contrastados.
- O brilho é sugerido pela textura; não depende de shader ou material PBR.
- Não usar grandes faixas brancas que façam a peça parecer metálica.

### Plástico poroso e cavidades

- Poros pequenos e discretos, lidos como textura mate e não como dezenas de furos.
- Separar a tampa do interior por contraste: preto poroso fora, preto profundo dentro.
- Um furo funcional pode ter recorte e rebaixo reais; a base de apoio continua sólida.

### Vidro colorido

- Lente de cor uniforme, com núcleo separado para comunicar iluminação.
- Base sólida e opaca; a transparência pertence à lente, não ao módulo inteiro.
- Mesmo formato e acabamento nos estados aceso e apagado. Não deslocar nem
  duplicar a geometria para fabricar o estado aceso.
- A emissão visual pertence à lâmpada específica; não à mesa nem à sua carcaça.
  Iluminação dinâmica do mundo permanece fora do escopo atual.

## 4. Formas, escala e proporções

O vocabulário principal é de **cuboides robustos e detalhes pixelados**. Cilindros
podem ser aproximados por facetas, como no buzzer. Não buscar curvatura lisa ou
pequenos conectores modelados com complexidade de produto real.

Usar relevo para tampa, moldura, antena, lente e conectores importantes. Pintar
detalhes secundários. Evitar acrescentar peças que desaparecem à distância de
uso normal ou só ficam legíveis num close extremo do Blockbench.

### Escala atual de referência

As medidas são unidades de modelo Minecraft: **16 unidades = 1 bloco**. Elas
não são pixels da resolução da textura; um PNG 64×64 não aumenta o módulo.

| Elemento | Dimensões/limites atuais | Papel na família |
| --- | --- | --- |
| Mesa | Espessura 3/16 de bloco, grid 3×3 | Plataforma fina; não um gabinete de instrumentos de um bloco inteiro. A textura de ferro ainda é placeholder. |
| Célula mínima | 16/3 ≈ 5,33 unidades por eixo | Footprint de ocupação, maior que a peça visível. Não arredondar para 5 ao calcular posicionamento. |
| Lâmpada 1×1 | 3,4 × 3,4, altura 2 | Referência de altura e volume dos pequenos módulos. |
| Lâmpada 2×1 | 6,8 × 3,4, altura 2 | Alongamento da mesma família; a mesa permite a orientação 1×2. |
| Buzzer 1×1 | Diâmetro aproximado 2,85, altura 1,9 | Um pouco menor que a lâmpada, mas com altura semelhante. |
| Radar Altimeter | Dentro de 0–16; altura máxima 15,75 | Carcaça quase do tamanho do bloco, com recuos, conectores e antena inferior distintos. |

**Footprint não é silhueta:** um módulo 2×1 ocupa duas células, mas não precisa
preencher toda a largura dessas células. Preservar respiro entre as peças e
uma altura baixa que mantenha o painel legível.

## 5. Orientação e legibilidade funcional

Todo sensor direcional precisa de uma face funcional inequívoca, reforçada por
pelo menos duas pistas: geometria/material próprio e marca de orientação.
Essas pistas devem funcionar sem ler documentação e sem shaders.

No Radar Altimeter, a referência é:

- antena clara com alvo em latão **embaixo**;
- setas laterais amarelas apontando para essa antena;
- tampa e emblema azul em cima;
- conectores decorativos na lateral, sem sugerir que ela seja a face de medição.

A decoração deve respeitar a direção real do dispositivo no código, inclusive
quando um sublevel gira. Para futuros scanners frontais, aplicar a mesma regra
à frente de medição; não reutilizar a antena inferior sem adaptar a orientação.

Setas amarelas são marcas estáticas. Azul de carcaça é identidade decorativa.
Nenhum dos dois deve parecer um LED que informa disponibilidade ou alerta.
Evitar texto miúdo obrigatório, siglas de fabricantes reais e painéis de
conectores cuja aparência sugira portas configuráveis que o mod não possui.

## 6. Convenções de modelagem e posicionamento

### Módulos da mesa

- Modelar na orientação canônica da mesa voltada para cima.
- **Centro horizontal em X/Z = 8; face de apoio em Y = 0.**
- O renderer posiciona a peça no centro do footprint e a eleva à superfície da
  mesa, atualmente Y = 3/16 de bloco. Não embutir esse deslocamento no modelo.
- A rotação da mesa para parede/teto é responsabilidade da integração. Não
  criar fontes deslocadas ou espelhadas para compensar cada face de montagem.
- Para variantes alongadas, preservar o centro. O 2×1 atual tem eixo longo em X;
  a integração o gira 90° para ocupar 1×2.
- Transformações de inventário/mão ficam no modelo de item, sem mover os cubos
  da fonte usada pelo renderer da mesa.

### Blocos independentes

- Usar a célula 0–16 e documentar face funcional, origem e eventuais rotações.
- A antena do altímetro tem centro inferior `(8, 0, 8)`, coincidente com os casts.
- Seleção e colisão devem acompanhar a geometria relevante do bloco; não deixar
  um cubo cheio invisível quando o equipamento possui recuos importantes.
- Não presumir orientação configurável: o altímetro atual tem direção inferior
  fixa no espaço local do bloco.

## 7. Texturas e renderização

### Densidade e estilo dos pixels

Priorizar consistência da **escala visual dos detalhes**, não uma resolução
única para todos os PNGs. As lâmpadas usam 16×16, o altímetro 32×32 e o buzzer
64×64. No buzzer, a resolução maior viabiliza facetas, poros e recorte do furo;
isso não estabelece um padrão de alta resolução para todos os próximos blocos.

- Usar pixels nítidos, sem antialiasing, desfoque ou textura fotográfica.
- Preferir padrões geométricos e realces em degraus; reflexos não precisam ser lisos.
- Ruído de material fica subordinado às formas grandes.
- Preservar uma densidade semelhante entre faces adjacentes; evitar UVs que
  transformem um parafuso quadrado em um retângulo sem intenção.
- Não exigir normal maps, bloom, reflexos ou PBR para a peça ser reconhecível.

### Escolha de material no jogo

| Família | Uso atual | Diretriz |
| --- | --- | --- |
| Carcaças e altímetro | Opaco | Padrão para peças sólidas, com sombreamento normal. |
| Buzzer | Cutout, alpha binário | Recorte para furo/silhueta; evitar transparência intermediária onde basta recorte. |
| Lâmpadas | Lente translúcida e emissão visual por módulo | Manter materiais separados; o material emissivo de prévia do Blockbench não implementa a emissão do jogo. |

Não criar faces coplanares sobrepostas, cascas duplicadas ou camadas
translúcidas desnecessárias. Faces recuadas e módulos da mesa não devem sumir
por culling de blocos vizinhos. Tratar isso na exportação e na integração.

A leitura sem shader é o requisito de base. Testar também com shaders como
verificação adicional: os glitches anteriormente observados nas lâmpadas com
Complementary Reimagined **não são considerados resolvidos por este guia**.
Não depender do bloom para diferenciar ligada de apagada, nem prometer que
apenas mudar a textura resolve ordenação/transparência em qualquer shader.

## 8. Fontes, fluxo e aceite visual

Manter `.bbmodel` editável, texturas incorporadas, exportador quando existente
e README de cada família. O JSON/PNG instalado em `src/main/resources` é o asset
de runtime; a fonte Blockbench precisa permanecer coerente com ele.

Para novos modelos, usar Blockbench Java Block/Item compatível com Minecraft
1.21.1. A referência técnica atual usa cubos/faces e apenas rotações exportáveis
nesse formato; não introduzir meshes ou materiais exclusivos da prévia sem
uma integração de renderização deliberada.

### Checklist por entrega

- [ ] Função reconhecível pela silhueta, mesmo antes de considerar a cor.
- [ ] Face funcional clara e consistente com a direção implementada.
- [ ] Materiais e acentos pertencem à paleta da família.
- [ ] Textura legível à distância de interação, não apenas em close.
- [ ] Peça centrada, apoio correto e nenhum offset compensatório no modelo.
- [ ] Módulos verificados em slots de canto/centro e mesas em cima, parede e teto.
- [ ] Variantes acesa/apagada preservam geometria; só a peça ativa muda de brilho.
- [ ] Inventário, mão e mundo exibem escala e orientação adequadas.
- [ ] Exportação, referências de textura, UVs e colisão verificados em testes quando aplicável.
- [ ] Assets carregam no cliente; validação visual in-game registrada separadamente do build.
- [ ] Se houver translucência, inspecionar vários ângulos e múltiplos módulos lado a lado.

### Referências locais

- [Lâmpada 1×1](../blockbench/lamp-module-1x1/README.md)
- [Lâmpada 2×1](../blockbench/lamp-module-2x1/README.md)
- [Buzzer 1×1](../blockbench/buzzer-module-1x1/README.md)
- [Radar Altimeter](../blockbench/radar-altimeter/README.md)
- [Terrain Scanner Basic](../blockbench/terrain-scanner-basic/README.md)
- [Terrain Scanner Advanced](../blockbench/terrain-scanner-advanced/README.md)
- [Avionics Cable e conector](../blockbench/avionics-cable/README.md)
- [Emissão visual das lâmpadas](lamp-emission.md)
- [Mesa e grid](modular-table.md)
