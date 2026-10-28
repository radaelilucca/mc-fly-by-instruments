# Fase 4 — GPWS Computer e motor de alertas

> **Arquivado (2026-10-28).** O GPWS Computer virou o Avionics Computer: sem perfis,
> modos nem regras editáveis. Veja [avionics-computer.md](avionics-computer.md) e o
> [plano do refactor](avionics-computer-plan.md). Este texto fica como histórico.

Este documento detalha a Fase 4 do roadmap do Aeronautics: Fly by Instruments.
Ele define o comportamento funcional do GPWS, mantendo a implementação dentro
do baseline atual do projeto. A Fase 4 entrega lógica e API; a apresentação
física final dos alertas continua sendo concluída na Fase 5.

## Objetivo e limites

Transformar snapshots dos instrumentos já implementados em estados de voo e
alertas úteis. O GPWS será um endpoint físico da Avionics Cable Network, com
configuração persistida por bloco e peripheral `gpws` para CC:Tweaked.

Esta fase não implementa o Annunciator Panel final, novos sensores físicos,
autopilot, nem qualquer raycast próprio do GPWS. Ele consome snapshots
existentes do Radar Altimeter e dos Terrain Scanners, sem aumentar a frequência
de scan nem carregar chunks.

## Entradas, cadência e qualidade dos dados

- Avaliar o estado do GPWS no mesmo intervalo configurável dos sensores,
  inicialmente a cada 5 ticks.
- Tratar leituras com mais de dois intervalos de sensor (10 ticks no padrão)
  como obsoletas.
- Combinar múltiplos Radar Altimeters usando o menor clearance válido.
- Usar o contato mais próximo conhecido entre Terrain Scanners válidos.
- Se uma fonte falhar, preservar as outras fontes válidas e publicar
  `SENSOR_FAIL` quando a fonte afetada for relevante/obrigatória.
- Rede incompleta ou limitada deve sinalizar `SENSOR_FAIL`; canais que dependem
  de dados ausentes ficam `UNAVAILABLE`.
- Cobertura parcial do scanner pode sustentar um alerta quando há um contato
  conhecido. Ausência de contato em cobertura parcial nunca significa caminho
  livre.
- Não exigir sensores opcionais para que um perfil básico produza alertas
  úteis. A ausência de Terrain Scanner, por exemplo, não deve impedir alertas
  baseados no Radar Altimeter nem gerar falha persistente por si só.

O GPWS não depende de `Velocity Sensor` ou `Gimbal Sensor` externos no baseline
atual. A velocidade vertical será estimada a partir do histórico de leituras
AGL do Radar Altimeter; o ângulo de bank será derivado da pose do sublevel
Sable. A orientação `FACING` do bloco GPWS define o eixo longitudinal usado
para interpretar roll. Sensores externos poderão ser considerados em uma
extensão futura, sem serem requisito para a experiência inicial.

## Perfis e operação

### Básico — padrão

- Perfil inicial recomendado ao jogador e utilizável sem configurar thresholds
  individuais, fases de voo ou roteamento de sensores.
- Descoberta automática dos sensores conectados e seleção automática de fase.
- Ativação automática quando a rede estiver completa e houver ao menos uma
  leitura válida suportada; sem dados válidos, permanece em `STANDBY` e expõe
  diagnóstico em vez de simular segurança.
- Deve oferecer alertas de clearance/descida usando apenas GPWS e Radar
  Altimeter. O Terrain Scanner amplia consciência de obstáculos, mas não é
  pré-requisito para a proteção básica.
- Configuração de jogador limitada a ativar/desativar e escolher sensibilidade
  global (`LOW`, `NORMAL`, `HIGH`).
- Após recarregar o mundo, retorna a `STANDBY` e volta a armar automaticamente
  quando dados válidos estiverem disponíveis.

### Avançado

- Permite thresholds por alerta/fase, histerese, tempos de confirmação,
  substituição manual da fase e ajustes permitidos de arming/inhibit.
- Exige comando explícito para entrar em `ARMED`; após reload permanece em
  `STANDBY`.
- A API CC:Tweaked oferece os mesmos controles autorizados pelo servidor; ela
  não é necessária para usar o perfil básico.

### Modos

- `OFF`: suprime os canais de risco.
- `STANDBY`: suprime os canais de risco, mas mantém diagnósticos e `SENSOR
  FAIL`.
- `ARMED`: avalia os canais normalmente.
- `TEST`: ativa sinteticamente todos os canais em `WARNING` até sair do teste;
  não modifica as leituras reais.

## Fases de voo e defaults

Fases: `GROUND`, `TAKEOFF`, `CLIMB`, `CRUISE`, `DESCENT`, `APPROACH` e
`UNKNOWN`. `UNKNOWN` aplica-se quando não há AGL válido.

Defaults iniciais, em blocos do jogo e blocos por segundo:

- `GROUND`: AGL ≤ 1,5 e módulo da velocidade vertical ≤ 0,5 por um segundo.
- `TAKEOFF`: subida > 1 por segundo com AGL < 8.
- `CLIMB`: subida acima de AGL 8.
- `APPROACH`: descida com AGL ≤ 32.
- `DESCENT`: descida acima de AGL 32.
- `CRUISE`: AGL > 8 e módulo da velocidade vertical ≤ 0,5 por segundo.
- `UNKNOWN`: nenhuma leitura AGL válida.

Uma fase candidata deve se manter por duas amostras antes da transição. A
detecção de `GROUND` requer a condição contínua de um segundo. Os números são
defaults de gameplay a calibrar em voo, não uma tentativa de reproduzir
limites aeronáuticos reais.

## Alertas iniciais

- `TERRAIN`: contato mais próximo na cobertura do scanner ≤ 32 blocos (`CAUTION`);
  em `APPROACH`, limite padrão de 8 blocos.
- `PULL_UP`: `WARNING` se contato dentro do cone central de 45° estiver a ≤ 8
  blocos, ou se o tempo estimado até o solo for ≤ 2 segundos durante descida.
- `SINK_RATE`: descida ≥ 6 blocos/s (`CAUTION`) ou ≥ 12 blocos/s (`WARNING`).
- `DON'T SINK`: perda ≥ 4 blocos em relação ao pico AGL registrado durante
  `TAKEOFF`/`CLIMB`, dentro de 5 segundos (`WARNING`).
- `TOO LOW`: fora de `GROUND`, AGL ≤ 8 (`CAUTION`) ou ≤ 3 (`WARNING`). Em
  `APPROACH`, usar 3 (`CAUTION`) e 1 (`WARNING`).
- `BANK ANGLE`: fora de `GROUND`, módulo do roll ≥ 30° (`CAUTION`) ou ≥ 45°
  (`WARNING`).
- `SENSOR FAIL`: `CAUTION` para fonte necessária ausente, falha, obsolescência
  ou rede incompleta/limitada. A ausência de equipamento opcional não é falha
  no perfil básico.

Sensibilidade `LOW`/`NORMAL`/`HIGH` desloca os limites em 20% na direção
apropriada. Aplicar 10% de histerese. Canais regulares ativam após duas
amostras qualificadoras; `PULL_UP` ativa na primeira. Limpar alertas após duas
amostras seguras além da histerese.

## Configuração e CC:Tweaked

O peripheral `gpws` deve expor:

- `getStatus()`, `getAlerts()` e `getConfiguration()`;
- `setMode(mode)`, `setProfile(profile)`, `setSensitivity(value)`;
- `setAlertConfiguration(channel, phase, config)`;
- `setPhaseOverride("auto" | phase)`.

Eventos somente em mudança de estado/configuração:
`gpws_alert_changed`, `gpws_flight_phase_changed`, `gpws_mode_changed` e
`gpws_configuration_changed`. Cada evento inclui o nome da attachment e o
snapshot pertinente. As chamadas de escrita são validadas e aplicadas pelo
servidor.

O perfil básico deve ser selecionável sem preencher thresholds; a tela ou
interação de jogo não deve exigir conhecimento dos nomes técnicos dos modos.
CC:Tweaked é uma interface paralela e avançada, não um pré-requisito para
receber alertas.

## Testes e aceite

- Testes unitários de canais, estados, severidades, thresholds, sensibilidade,
  histerese, amostras de confirmação, fase, velocidade vertical e roll.
- Testes de fusão de múltiplos sensores, cobertura parcial, ausência, falha,
  obsolescência e recuperação das leituras.
- Testes de rede incompleta/limitada e de sensores opcionais ausentes sem
  bloquear o perfil básico baseado em Radar Altimeter.
- Testes de persistência de perfil/configuração e reset seguro do modo após
  reload.
- Testes automatizados do contrato Lua: métodos, validação de escrita,
  snapshots e eventos emitidos apenas quando há mudança.
- Testes de desempenho na cadência configurada, sem raycasts adicionais ou
  carregamento de chunks.
- Validação in-game em contraptions pequenas e grandes: voo nivelado,
  aproximação, obstáculo, descida, inclinação e somente Radar Altimeter no
  perfil básico.
- Calibrar defaults em voo antes de aceitar a fase; evitar alertas intrusivos
  em manobras normais de gameplay.

Critério de saída: os sete canais iniciais funcionam; perfis básico e avançado
são utilizáveis; APIs/eventos CC:Tweaked e testes nascem junto com o GPWS; e o
perfil básico demonstra valor sem exigir scanner avançado, ajuste técnico ou
conhecimento de aviação. A apresentação visual/sonora completa fica para a
Fase 5.

## Diretriz de experiência arcade-first

Os instrumentos podem parecer físicos e permitir configuração profunda, mas o
jogador não deve ter de montar ou operar uma simulação aeronáutica para obter
proteção útil. A Fase 4 deve validar a lógica simplificada; a Fase 5 deve
fornecer um caminho igualmente simples para perceber os alertas (por exemplo,
um setup padrão/rápido do Annunciator Panel). Esse setup de apresentação ainda
precisa ser decidido antes do detalhamento da Fase 5.
