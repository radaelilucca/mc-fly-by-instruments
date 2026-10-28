# Plano de ação — revisão dos sensores (redstone + CC:Tweaked)

Origem: revisão de 2026-10-21 da implementação do Radar Altimeter, do Terrain Scanner e
do Advanced Terrain Scanner, no escopo do build simplificado (saída de redstone + CC).
Cada achado foi conferido no código; o achado SEN-01 também foi conferido contra o
`ObserverBlock` e o `DiodeBlock` do Minecraft. Nada foi reproduzido no jogo até aqui.

Severidades: **Crítico** quebra um caso de uso principal; **Alto** produz leitura ou
comportamento enganoso; **Médio** é limitação de robustez ou usabilidade; **Baixo** é
polimento ou manutenção.

## Atualização de 2026-10-28

A tela de saída dos sensores foi refeita no padrão HUD (guia visual, seção 12), com
mudanças aplicadas na hora. Isso fecha **SEN-02** (o servidor mede o alcance no mundo,
com a pose do Sable, também para o Avionics Computer) e **UX-04** (o cabeçalho mostra
`Faces do bloco` / `Via Link`), e resolve **UX-06** de outro jeito (com abertura ≤ 50°
as laterais só mostram o aviso). **UX-02** perde o objeto (não há mais Salvar que
feche a tela); recusas do servidor continuam silenciosas. **UX-01** continua aberto e
agora deve mostrar a leitura como linha do painel, não sobre um slider.

## Resumo

| ID | Sev. | Achado | Fase |
| --- | --- | --- | --- |
| SEN-01 | Crítico | Advanced emite os canais pelas faces erradas | 1 |
| SEN-02 | Crítico | Salvar config de sensor em sublevel é descartado (distância sem Sable) | 1 |
| SEN-03 | Alto | Leitura indisponível/falha vira 0, que já significa "chão" ou "livre" | 2 |
| UX-02 | Alto | Salvar fecha a tela sem confirmação; rejeições silenciosas | 1 |
| UX-01 | Alto | Tela não mostra leitura nem potência atual | 3 |
| SEN-04 | Médio | Modo Local/Link global: um par completo desliga todas as faces | 2 |
| SEN-05 | Médio | Endpoint do Redstone Link via `Proxy` + reflexão | 4 |
| SEN-06 | Médio | Config perdida ao recolher; Wrench não gira scanners | 3 |
| UX-03 | Médio | Polaridades opostas entre altímetro e scanners, sem opção de inverter | 2 |
| UX-04 | Médio | Modo Local/Link invisível na tela | 3 |
| UX-05 | Médio | Fenda de redstone não acende (estado `powered` não ligado) | 3 |
| SEN-07 | Baixo | Saídas não persistidas: pulso de 0 após load; clamp fixo em 128 no construtor | 4 |
| SEN-08 | Baixo | API CC: no-op que retorna `true`, erros sem motivo, sem limites, tipo do Advanced | 4 |
| SEN-09 | Baixo | Código morto da tela de ângulo antiga | 4 |
| UX-06 | Baixo | Abas ESQ/DIR editáveis com abertura ≤ 50° | 3 |
| UX-07 | Baixo | Altímetro ignora fluidos e mede o fundo sobre água | 2 |
| UX-08 | Baixo | Textos misturam português e inglês | 4 |
| UX-09 | Baixo | Pó de redstone conecta na traseira do Advanced, que não emite | 3 |

Verificados e **descartados**: alcance do Redstone Link entre sublevel e mundo (o Sable
redireciona `RedstoneLinkNetworkHandler.withinRange` e projeta as posições) e identidade
de frequência (o Create compara só item + cor tingida, igual ao nosso protocolo).

## Decisões em aberto

Precisam de resposta antes da fase indicada:

| ID | Decisão | Proposta | Fase |
| --- | --- | --- | --- |
| D-1 | Política de falha padrão | Altímetro: acima do alcance = 15, falha = manter último valor. Scanners: indisponível/falha = 15 (fail-safe: "obstáculo"). Configurável por sensor | 2 |
| D-2 | Roteamento por canal | Cada canal usa Link se o par dele estiver completo; senão, a face. Remove o modo global | 2 |
| D-3 | Inversão | Toggle "inverter" por canal (15 − valor), aplicado depois da política de falha | 2 |
| D-4 | Fluidos no altímetro | Opção por bloco "medir até a água", padrão ligado | 2 |
| D-5 | Granularidade do `powered` | Uma propriedade booleana (qualquer canal > 0). Acender por canal no Advanced fica fora | 3 |
| D-6 | Engineer's Goggles | Implementar `IHaveGoggleInformation` (Create) com leitura, potência e modo | 3 |

## Fase 1 — Correções críticas

### SEN-01 · Faces do Advanced

- **Arquivos:** `sensor/AbstractTerrainScannerBlock.java` (`getSignal`),
  `sensor/SensorOutputLogic.java`, `SensorOutputLogicTest.java`.
- **Correção:** `getSignal` recebe a direção do vizinho para o bloco. Converter para a
  face de saída antes de mapear o canal: `scanner.outputPower(direction.getOpposite())`.
  Renomear o parâmetro de `weakPower` para `outputFace` e documentar a convenção.
- **Testes:**
  - Corrigir `advancedOutputsFollowFacingAndBasicRepeatsOnEveryFace` para chamar pelo
    caminho do bloco, não pela função pura.
  - GameTest: Advanced nas 4 direções e no teto, pó de redstone em cada face, contato
    sintético à frente, à esquerda e à direita. Confirmar que FRENTE sai na face frontal
    e ESQUERDA/DIREITA na face correspondente, e que a traseira e o topo ficam em 0.
- **Aceite:** GameTest verde nas 8 orientações; Basic e altímetro sem mudança.

### SEN-02 · Alcance com Sable ao salvar

- **Arquivos:** `network/SensorOutputNetwork.java`; reaproveitar `panel/PanelSpatial` ou
  extrair um `SpatialAccess.canReach(player, blockEntity)` comum. Aplicar também em
  `network/GpwsNetwork.java` (experimental, mesmo defeito).
- **Correção:** projetar a posição do bloco pelo `logicalPose` do sublevel antes de medir
  a distância, como a mesa já faz.
- **Testes:** unitário do helper (bloco no mundo, bloco em sublevel deslocado);
  GameTest com sensor em sublevel recebendo `ApplySensorOutputConfigPayload`.
- **Aceite:** config salva a partir da tela num navio; distância de 8 blocos respeitada.

### UX-02 · Confirmação do salvamento

- **Arquivos:** `SensorOutputNetwork.java`, novo payload de resposta,
  `client/SensorOutputConfigScreen.java`.
- **Correção:** o servidor responde aceito ou recusado (com motivo: fora de alcance,
  bloco mudou, config inválida). A tela só fecha ao receber "aceito"; em recusa, mostra
  a mensagem e mantém o rascunho. ESC continua aplicando sem bloquear o fechamento, mas
  mostra um toast se for recusado.
- **Aceite:** recusa visível; nenhuma reabertura de tela após o fechamento (manter a
  proteção atual contra reabrir).

## Fase 2 — Semântica da saída

Pré-requisitos: D-1, D-2, D-3 e D-4.

### SEN-03 · Política de falha

- **Arquivos:** `sensor/SensorOutputConfig.java` (novo campo por canal ou por sensor),
  `SensorOutputLogic.java`, `SensorOutputController.java`, NBT e wire.
- **Correção:** separar os casos de leitura:
  - altímetro sem retorno dentro do alcance = "acima do alcance";
  - indisponível por chunk descarregado ou falha = "sem dados".
  Aplicar a política escolhida (manter, 0, 15) em vez de forçar 0. Ao descarregar o chunk
  em modo Link, o endpoint sai da rede e os receptores caem para 0; documentar isso como
  limitação do Create.
- **Testes:** casos unitários para cada estado e política; teste da curva do altímetro
  (monotônica até e acima do alcance).
- **Aceite:** voar acima de 128 blocos não leva o altímetro a 0; chunk descarregado à
  frente não aparece como "livre" com a política padrão.

### SEN-04 · Roteamento por canal

- **Arquivos:** `SensorOutputConfig.java` (remover `Mode` global ou mantê-lo só para
  leitura), `SensorOutputLogic.weakPower`, `SensorOutputController.refreshLinks`,
  `SensorOutputLua`.
- **Correção:** cada canal com par completo transmite por Link; os demais continuam nas
  faces. Migrar NBT antigo: `Mode=LINK` vira "Link nos canais com par completo".
- **Testes:** Advanced com Link só na FRENTE mantém ESQUERDA/DIREITA nas faces; carga de
  NBT antigo.
- **Aceite:** nenhum canal fica mudo por causa da configuração de outro.

### UX-03 · Inverter por canal

- **Arquivos:** `SensorOutputConfig` (flag por canal), `SensorOutputLogic`, tela, CC
  (`setOutputInverted(channel, bool)`), NBT e wire.
- **Aceite:** altímetro invertido dá 15 perto do chão; a inversão também vale para o Link.

### UX-07 · Água no altímetro

- **Arquivos:** `sensor/CollisionRaycaster.java` (`ClipContext.Fluid`),
  `RadarAltimeterScanner`, config por bloco, tela, CC.
- **Aceite:** sobre o mar, com a opção ligada, mede até a superfície; desligada, mantém o
  comportamento atual.

## Fase 3 — Feedback ao jogador

### UX-01 · Leitura ao vivo

- **Arquivos:** `SensorOutputConfigScreen.java`, payload de status servidor→cliente
  enquanto a tela está aberta (throttle de 4 Hz), `TerrainScannerBlockEntity` e
  `RadarAltimeterBlockEntity`.
- **Conteúdo:** leitura atual (AGL ou distância por setor), estado (válido, indisponível,
  falha) e potência de cada canal, marcada sobre o slider da faixa.
- **Aceite:** ajustar o slider mostra na hora qual potência a leitura atual produziria.

### D-6 · Engineer's Goggles

- **Arquivos:** os três block entities (`IHaveGoggleInformation`).
- **Conteúdo:** leitura, potência por canal, modo de cada canal e política de falha.
- **Aceite:** informações aparecem com os Goggles, inclusive em sublevel.

### UX-04 · Modo visível

- **Arquivos:** `SensorOutputConfigScreen.java`, idioma (`sensor_output.mode_link` e
  novas chaves).
- **Correção:** indicador por canal ("Faces" ou "Link A/B") e aviso de par incompleto.

### UX-05 · Fenda acesa

- **Arquivos:** os três blocos (propriedade `POWERED`), blockstates (`_powered` já
  exportados), `SensorOutputController` (atualizar o estado só quando cruzar 0 ↔ >0),
  `RadarAltimeterAssetsTest` (remover a exigência de propriedades vazias, mantendo a
  ausência de direção configurável).
- **Aceite:** fenda acesa com qualquer canal > 0; sem flood de atualização de bloco a
  cada scan.

### SEN-06 · Girar e preservar config

- **Arquivos:** `block/AvionicsWrenchable.java` (scanners: girar `FACING` com o Wrench),
  loot tables dos três sensores (copiar os dados do block entity para o item) e
  `saveToItem`/componentes.
- **Aceite:** Wrench gira o scanner sem perder a config; recolher e recolocar preserva
  faixas, frequências, abertura e políticas.

### UX-06 e UX-09

- **UX-06:** desabilitar visualmente as abas ESQ/DIR com abertura ≤ 50°, com o motivo na
  própria aba.
- **UX-09:** `canConnectRedstone` do Advanced só nas faces que emitem.

## Fase 4 — Robustez e manutenção

### SEN-05 · Endpoint do Link

- **Arquivos:** `SensorOutputController.java`.
- **Correção:** trocar o `Proxy` por uma classe que implementa `IRedstoneLinkable` e usa
  `net.createmod.catnip.data.Couple` diretamente (já no classpath via Ponder). Remover a
  reflexão.
- **Aceite:** compila contra a API do Create; uma mudança futura na interface quebra o
  build, e não o tick do servidor.

### SEN-07 · Estado inicial

- **Correção:** persistir a última saída no NBT e restaurá-la no `onLoad`; usar o
  alcance máximo configurado no servidor também na construção do controller.

### SEN-08 · API CC

- Deprecar `setOutputMode` (retornar `false` com motivo, ou remover numa versão maior).
- Lançar `LuaException` com motivo nas falhas de `setOutputRange`, `setLinkFrequencies`
  e `setOpeningAngle`.
- Novo `getLimits()`: alcance máximo, canais válidos, faixa de abertura e políticas
  disponíveis.
- Advanced: `getAdditionalTypes()` com `terrain_scanner`, para `peripheral.find`.
- Expor as novas opções (política de falha, inverter, fluidos) e o modo por canal.
- Atualizar `docs/radar-altimeter.md` e `docs/terrain-scanner.md` com a API final.

### SEN-09 · Código morto

- Remover `client/AdvancedTerrainScannerScreen.java`, `client/TerrainScannerClient.java`,
  `network/TerrainScannerNetwork.java`, os payloads `TerrainScannerSetAnglePayload` e
  `TerrainScannerScreenPayload`, o registro em `AeronauticsFlyByInstruments` e as chaves
  de idioma `gui.*.terrain_scanner.*` sem uso.

### UX-08 · Textos

- Padronizar títulos e nomes da tela com os nomes dos itens em `pt_br` e `en_us`.

## Validação final (antes do build simplificado)

- `.\gradlew.bat test` e GameTests verdes.
- No jogo, com cada sensor no chão, no teto e num navio em movimento:
  - faces e Link em todos os canais;
  - política de falha voando acima do alcance e diante de chunk não carregado;
  - tela: salvar, recusar, leitura ao vivo;
  - Goggles e fenda acesa;
  - Wrench e recolhimento.
- Multiplayer: dois jogadores editando o mesmo sensor; a última gravação vence e a tela
  aberta do outro é atualizada.
- Registrar o resultado em `docs/TODO.md` (QA-01 e QA-03).
