# GPWS Computer

O GPWS Computer é o endpoint que interpreta os snapshots já produzidos pelo
Radar Altimeter e pelos Terrain Scanners. Ele não dispara raycasts próprios,
não carrega chunks e avalia a rede na mesma cadência configurável dos sensores.
Leituras com mais de dois intervalos de amostragem são consideradas obsoletas.

## Uso no jogo

O gabinete (v2.1) é de ferro industrial com acabamento de latão, sem madeira:
base de ferro, corpo com venezianas e rebites de latão, tampa de latão e, na
frente, uma tela no estilo do CC:Tweaked e o botão de teste. Com o GPWS em
`ARMED`, um cursor pisca na tela; nos outros modos, ela mostra só o prompt. O
botão é decorativo: os controles funcionais continuam na interface. O cabo
conecta em qualquer face.
A fonte editável fica em [Blockbench](../blockbench/gpws-computer/README.md).

Conecte o GPWS Computer e os sensores ao mesmo Avionics Cable Network. Interaja
com o computador para abrir os controles:

- **Básico** é o padrão. Ao encontrar uma rede completa e um Radar Altimeter
  válido, arma automaticamente. Não requer scanner de terreno, CC:Tweaked ou
  configuração de thresholds. Permite ligar/desligar e escolher sensibilidade.
- **Avançado** começa em `STANDBY`; o jogador precisa armar explicitamente.
  Thresholds por canal/fase, histerese, confirmação e fase manual são
  configurados pelo peripheral CC:Tweaked.
- A opção **Teste** ativa sinteticamente todos os canais em `WARNING`, sem
  alterar as leituras. No perfil Básico, use **Ativar** para sair do teste.
- Após carregar/recarregar o mundo, o modo retorna a `STANDBY`. O Básico arma
  novamente quando a rede e o altímetro estiverem válidos; o Avançado exige
  novo comando para armar.

`LOW`, `NORMAL` e `HIGH` deslocam em 20% os limites na direção de maior ou
menor sensibilidade. Limites de distância/clearance crescem em `HIGH`; limites
de velocidade de descida, perda de altitude e bank diminuem em `HIGH`.

## Fases automáticas

Fases disponíveis: `GROUND`, `TAKEOFF`, `CLIMB`, `CRUISE`, `DESCENT`,
`APPROACH` e `UNKNOWN`. A velocidade vertical é estimada pelo histórico do
menor clearance válido entre os altímetros conectados, em blocos por segundo.
As mudanças comuns de fase exigem duas amostras consecutivas; `GROUND` exige
AGL ≤ 1,5 bloco e velocidade vertical absoluta ≤ 0,5 bloco/s durante 20 ticks.
`UNKNOWN` também pode aparecer durante a inicialização, antes de haver amostras
suficientes para estimar a velocidade vertical. Enquanto a condição de solo é
confirmada, os canais de risco ficam quietos para evitar um aviso transitório
de `TOO LOW` ao ligar o sistema com a aeronave estacionada.

## Canais e limites padrão

Os valores abaixo são parâmetros de gameplay em blocos, blocos por segundo,
segundos e graus — não limites aeronáuticos reais.

| Canal | Comportamento inicial |
|---|---|
| `TERRAIN` | `CAUTION` quando o contato conhecido mais próximo está a ≤32 blocos; em `APPROACH`, ≤8. Sem scanner, ou sem cobertura suficiente e sem contato conhecido, fica `UNAVAILABLE`. |
| `PULL_UP` | `WARNING` imediato para contato conhecido a ≤8 blocos dentro do cone central frontal de 45° (±22,5°), ou tempo estimado até o solo ≤2 s durante descida. Um contato conhecido pode alertar mesmo com cobertura parcial. |
| `SINK_RATE` | `CAUTION` a partir de 6 blocos/s de descida; `WARNING` a partir de 12 blocos/s. |
| `DON'T SINK` | `WARNING` por perda ≥4 blocos frente ao pico AGL recente durante `TAKEOFF`/`CLIMB`, observado numa janela de 5 s. |
| `TOO LOW` | Fora de `GROUND`: `CAUTION` a ≤8 blocos AGL ou `WARNING` a ≤3; em `APPROACH`, ≤3 e ≤1, respectivamente. |
| `BANK ANGLE` | Fora de `GROUND`: `CAUTION` a partir de 30° e `WARNING` a partir de 45°. O bank vem da pose do sublevel Sable e do `FACING` do GPWS Computer. |
| `SENSOR FAIL` | `CAUTION` se a rede não estiver conectada/completa, se faltar o Radar Altimeter obrigatório ou se uma fonte instalada relevante estiver falhando/obsoleta. A ausência de Terrain Scanner, por si só, não é falha. |

Os canais de risco são suprimidos em `GROUND`, `STANDBY` e `OFF`. `STANDBY`
mantém o diagnóstico `SENSOR_FAIL`; `OFF` suprime todos os alertas. A Fase 4
entrega estados para integração; lâmpadas e buzzers físicos pertencem à Fase 5.

Cobertura parcial com contato pode sustentar um alerta. A ausência de contatos
em cobertura parcial não é interpretada como caminho livre: o canal dependente
fica `UNAVAILABLE`. Para provar cobertura frontal do `PULL_UP`, é necessário
um scanner frontal alinhado ao GPWS com varredura completa e abertura mínima
de 45°; sem Terrain Scanner, a lógica de tempo até o solo continua utilizável.

Aplicam-se 10% de histerese por padrão. Os canais regulares exigem duas
amostras qualificadoras para ativar e duas amostras seguras para limpar;
`PULL_UP` ativa na primeira amostra.

## Peripheral CC:Tweaked

Tipo do peripheral: `gpws`.

| Método | Resultado/efeito |
|---|---|
| `getStatus()` | modo, perfil, fase automática/efetiva, estado da rede, contagem de endpoints, clearance, velocidade vertical e bank com status/unidade/tempo. |
| `getAlerts()` | tabela por canal com `state` (`inactive`, `active`, `unavailable`), `severity` e `gameTime`. |
| `getConfiguration()` | perfil, sensibilidade, fase manual e regras efetivas por canal/fase. |
| `setMode(mode)` | `off`, `standby`, `armed` ou `test`; `armed` explícito só no perfil avançado. |
| `setProfile(profile)` | `basic` ou `advanced`; mudar o perfil retorna para `standby`. |
| `setSensitivity(value)` | `low`, `normal` ou `high`. |
| `setAlertConfiguration(channel, phase, config)` | No Avançado, substitui a regra de uma combinação canal/fase. |
| `setPhaseOverride("auto" ou phase)` | No Avançado, seleciona `ground`, `takeoff`, `climb`, `cruise`, `descent`, `approach` ou `unknown`. |

Os campos aceitos em `config` são `cautionThreshold`, `warningThreshold`,
`secondaryWarningThreshold`, `hysteresisPercent`, `confirmationSamples` e
`inhibited`. Limites iguais a zero desativam aquele nível. A semântica é por
canal: `TERRAIN` usa distância; `PULL_UP` usa distância central em
`warningThreshold` e tempo até o solo em `secondaryWarningThreshold`;
`SINK_RATE` usa velocidade de descida; `DONT_SINK` usa perda de clearance;
`TOO_LOW` usa AGL; `BANK_ANGLE` usa graus. `confirmationSamples` aceita 1–20,
`hysteresisPercent` 0–90 e thresholds finitos de 0–4096. Regras de canal
inibidas ficam imediatamente inativas. Escritas inválidas ou no perfil Básico
retornam `false` e não alteram o estado.

Exemplo simples:

```lua
local gpws = peripheral.find("gpws")
if not gpws then error("GPWS Computer não encontrado") end

gpws.setProfile("basic")
gpws.setSensitivity("normal")
print(textutils.serialize(gpws.getStatus()))

while true do
  local event, attachment, changed = os.pullEvent("gpws_alert_changed")
  print(attachment, textutils.serialize(changed))
end
```

Exemplo avançado:

```lua
local gpws = peripheral.find("gpws")
gpws.setProfile("advanced")
gpws.setAlertConfiguration("sink_rate", "descent", {
  cautionThreshold = 5,
  warningThreshold = 10,
  hysteresisPercent = 12,
  confirmationSamples = 3,
})
gpws.setPhaseOverride("auto")
gpws.setMode("armed")
```

Eventos emitidos somente quando o estado/severidade, fase, modo ou configuração
muda: `gpws_alert_changed`, `gpws_flight_phase_changed`, `gpws_mode_changed` e
`gpws_configuration_changed`. Cada evento leva primeiro o nome da attachment
e depois um snapshot do que mudou. Alterações são aplicadas no servidor.
