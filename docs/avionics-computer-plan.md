# Plano — Avionics Computer e simplificação da mesa

Status: **implementado em 2026-10-28** (fases 1–9; build e testes automatizados verdes,
validação no jogo pendente). Documentação de uso: [avionics-computer.md](avionics-computer.md)
e [modular-table.md](modular-table.md). Substitui [gpws-phase-4-plan.md](gpws-phase-4-plan.md).

### Onde a implementação divergiu do plano

- **Sem canais `gpws.warning`/`gpws.caution`/`gpws.inop`.** As indicações da mesa são
  gravadas nos campos que já existiam (Alerta = `gpws.any` ≥ 3, Atenção = `gpws.any` ≥ 2,
  INOP = `gpws.sensor_fail`), o que mantém saves e scripts antigos. A lâmpada INOP também
  acende quando nenhum computador responde num cabo completo.
- **Funções:** os canais existentes foram mantidos. "Terrain ahead" é o canal `terrain`
  (scanner) e "Pull up" junta terreno à frente e tempo até o solo; não há uma função
  separada de "taxa de fechamento".
- **Função específica** aparece como uma seção da lista na tela do módulo, não como submenu
  (um clique a menos).
- **Redstone Link reservado:** só a fonte `redstone_link` nos dados (carrega e salva, nunca
  acende) e a opção "em breve" na tela. Frequências e cor escolhida entram com o modo.
- **Volume do buzzer** é enviado ao fechar a tela; a indicação aplica no clique.
- **Correção encontrada no caminho:** o estado dos alertas da mesa nunca era sincronizado
  com o cliente, então lâmpadas ligadas ao GPWS não acendiam para o jogador. Agora vai na
  atualização da mesa (sem ser salvo em disco).

## 1. Por quê

O GPWS Computer atual é difícil de operar até para quem o escreveu: perfis Básico/Avançado,
modos OFF/STANDBY/ARMED/TEST, sensibilidade, fase manual e 49 regras (7 canais × 7 fases)
editáveis só por Lua. A tela não mostra sensores, leituras nem alertas. A mesa repete o
problema: cada módulo escolhe fonte, canal (8), severidade mínima (3) e ainda tem campos de
texto livres para sinal de teste.

Um GPWS real é o oposto: o piloto quase não o configura nem o opera. Ele fica ativo sempre
que tem energia e entradas válidas, usa envelopes fixos de certificação, mostra INOP por
função quando falta uma entrada e oferece só TEST e chaves de inibição. A configuração
acontece uma vez, na instalação (*program pins*). Os alertas têm prioridade e poucos
anunciadores: vermelho (PULL UP/warning), âmbar (caution) e INOP.

**Princípio do refactor: configura uma vez, depois só funciona.**

## 2. Decisões fechadas

| Tema | Decisão |
| --- | --- |
| Identidade | O bloco vira **Avionics Computer**; GPWS é uma função dele |
| ID | `gpws_computer` → `avionics_computer`, com alias do ID antigo (bloco, item, block entity) |
| Peripheral | Tipo `gpws` → `avionics_computer`; GPWS vira seção da API |
| Canais na rede | Continuam `gpws.*`; entram os anunciadores `gpws.warning`, `gpws.caution`, `gpws.inop` |
| Modelo do bloco | Mantém o gabinete v2.1; botão TEST passa a funcionar; tela do bloco mostra estado |
| Network Bridge | **Fica separada**: só topologia, diagnóstico da fiação |
| Gateway | Avionics Computer é o gateway da leitura geral da rede (todos os sensores + alertas) |
| Peripherals dos sensores | **Ficam.** Fluxos custom com um único sensor e um computador CC continuam possíveis |
| Thresholds | Envelopes fixos + sensibilidade global (baixa/normal/alta). Saem perfis, ARMED/STANDBY, fase manual e regras por canal/fase |
| Ligação das lâmpadas | Fica na mesa, com lista curta de **indicações** |
| Reconhecer alerta | Clique de mão vazia em lâmpada **acesa** reconhece (silencia buzzers); em lâmpada **apagada** abre a configuração |
| Cor da lâmpada | Vermelho e âmbar, escolhidos pela indicação (seção 6) |
| Configurar no mundo | Scroll sobre o módulo troca a indicação, com rótulo flutuante; a tela continua existindo |
| Redstone Link | Modo futuro da lâmpada, **reservado** no modelo de dados e na UI desde já |
| Linguagem de UI | Limpa, sem ruído, baseada na HUD fixa do inspector do Exotic Pets (seção 4) |

## 3. Funções do computador

Cada função é fixa, com envelope em blocos/segundos ajustado só pela sensibilidade. Na tela
e na API cada uma tem um estado: **ativa**, **alerta** (com severidade), **INOP** (com
motivo) ou **inibida**.

| Função | Modo real | Entradas | Saída |
| --- | --- | --- | --- |
| Sink rate | 1 | Altímetro | caution → warning |
| Terrain / Pull up (aproximação) | 2 | Altímetro (taxa de fechamento) | caution → warning |
| Don't sink | 3 | Altímetro (perda após decolagem) | warning |
| Too low | 4 | Altímetro | caution → warning |
| Terrain ahead | FLTA (EGPWS) | Scanner frontal | caution → warning |
| Bank angle | 6 | Pose do sublevel | caution → warning |
| GPWS INOP | — | Rede e sensores | inop |

As 7 fases de voo viram um estado interno invisível (solo / decolagem / voo / aproximação),
usado só para escolher envelopes e suprimir alertas no solo. Prioridade de alerta:
Pull up > Terrain ahead > Terrain > Too low > Sink rate > Don't sink > Bank angle.

**Papéis dos sensores** são atribuídos automaticamente: o altímetro mais baixo vira
"principal"; o scanner alinhado ao computador vira "frontal"; os outros, "lateral E/D" ou
"auxiliar". Problemas de instalação aparecem na própria linha do sensor ("desalinhado",
"abertura < 45°", "outro sublevel", "obsoleto"). O alinhamento exigido hoje (0,5°) passa
a ser explicado na tela, em vez de só desligar o Pull up em silêncio.

## 4. Linguagem de UI

Base: a HUD fixa do inspector de pets
(`../pets/src/main/java/com/radaeli/exoticpets/client/inspector/PetInspectorHud.java`).

- Painel escuro da paleta `UiTheme.DARK` da R-UI Lib, quase opaco, contorno de 1 px e
  **faixa de acento de 2 px** à esquerda.
- Cabeçalho: título à esquerda, metadado discreto alinhado à direita.
- Uma linha de destaque com a cor de acento (no computador: o estado geral).
- Seções com título curto em maiúsculas, cor apagada, separadas por um divisor fino.
- Linhas: rótulo apagado à esquerda, valor alinhado à direita, cortado com reticências.
- Valor que acabou de mudar acende na cor de aviso e esmaece.
- Cores só com função: sucesso, aviso, erro. Nada de caixas, ícones ou textos de ajuda
  permanentes; dicas aparecem só por alguns segundos ou em tooltip.
- Ações são poucas e ficam no rodapé. Escolhas aplicam na hora: sem Salvar/Cancelar.

Essa regra vale para o mod todo e deve entrar no guia visual (nova seção de telas) junto da
fase 4.

## 5. Telas

### 5.1 Avionics Computer

```
▌ Avionics Computer                  Sensib. normal
▌ » ATENÇÃO · TERRAIN AHEAD
▌ ───────────────────────────────────────────────
▌ SENSORES
▌ Altímetro principal                       14,2 m
▌ Scanner frontal                             22 m
▌ Scanner lateral E                    desalinhado
▌ ───────────────────────────────────────────────
▌ FUNÇÕES
▌ Sink rate                                  ativa
▌ Terrain ahead                            atenção
▌ Too low                                 inibida
▌ Bank angle                  INOP: fora de sublevel
▌                        [ Sensibilidade ] [ Testar ]
```

- Atualiza ao vivo enquanto aberta (payload periódico do servidor, só para quem está com a
  tela aberta).
- Clicar numa função alterna inibida/ativa. Clicar em "Sensibilidade" cicla baixa → normal
  → alta.
- O botão físico do modelo também testa. A tela do bloco troca de textura por estado
  (normal / alerta / INOP), com a técnica de blockstate + textura animada da seção 9.4 do
  guia.

### 5.2 Mesa, célula vazia

```
▌ Painel                            computador OK
▌ Módulos                            6 · 1 aceso
▌ Buzzers                                 ativos
▌ ───────────────────────────────────────────────
▌ ACESAS
▌ Lâmpada 2, 1                       Alerta GPWS
▌                     [ Testar painel ] [ Silenciar ]
```

A seção "Acesas" lista só as lâmpadas acesas, com a indicação de cada uma; sem lâmpada
acesa, ela some. Sai a lista completa de módulos e a "Configuração básica" (substituída
por defaults corretos).

### 5.3 Lâmpada e buzzer

```
▌ Lâmpada 2, 1                            apagada
▌ ACENDE QUANDO
▌ ● Alerta GPWS                          vermelho
▌ ○ Atenção GPWS                            âmbar
▌ ○ GPWS INOP                               âmbar
▌ ○ Função específica  ›
▌ ○ Redstone Link                        em breve
```

- **Lâmpada:** escolhe uma indicação. A severidade fica implícita. "Função específica" abre
  a lista das funções da seção 3. "Redstone Link" aparece reservado, sem ação.
- **Buzzer:** só volume e "tocar em: todos os alertas / só vermelhos". O padrão de bip muda
  com a severidade (rápido em warning, lento em caution).
- Defaults: lâmpada nova = Atenção GPWS; buzzer novo = todos os alertas.
- Sinais de teste locais saem da tela e continuam só no Lua.

### 5.4 Configuração no mundo

- Olhando para um módulo, segurando a **wrench do Create** (obrigatória), o scroll cicla
  as indicações principais: Alerta GPWS → Atenção GPWS → (Redstone Link, quando existir).
  A wrench evita roubar o scroll da hotbar. Funções específicas e INOP ficam só na tela.
- Um rótulo flutuante no estilo da seção 4 mostra a indicação atual e a próxima, ao lado
  do módulo, como os value boxes do Create.
- No buzzer, o scroll ajusta o volume em passos de 10%.

### 5.5 Telas dos sensores

As telas de saída (redstone e Link) estão na entrega atual e não mudam, exceto por uma
linha: "Avionics Computer: Scanner frontal" (ou "não conectado").

## 6. Lâmpada vermelha e âmbar

A seção 9.4 do guia já define as cores: warning = rampa de redstone; caution = âmbar com os
tons claros do latão (`#E4B763`–`#FFEB8C`), trocando só o vidro. Proposta de execução:

1. **Duas texturas de vidro com o mesmo mapa de alpha:** `lamp_glass_red` (existente) e
   `lamp_glass_amber`. Mudam só os tons, com miolo, bordas e coluna de reflexo nas mesmas
   posições. Assim as duas cores leem como o mesmo objeto.
2. **Dois filamentos acesos:** `lamp_filament_on` (xadrez de redstone) e
   `lamp_filament_on_amber` (xadrez com os tons claros do latão). Um filamento vermelho
   atrás de vidro âmbar ficaria laranja e confundiria as duas cores. O filamento apagado é
   o mesmo nas duas.
3. **Cor visível apagada:** o vidro mantém a cor mesmo desligado, como os anunciadores
   reais. O jogador sabe qual lâmpada é qual sem acender.
4. **Modelos:** cada tamanho ganha `_amber_off` e `_amber_on` (1×1 e 2×1), gerados pelo
   mesmo `export-assets.mjs`. O item escolhe o modelo por `CustomModelData` (0 vermelho
   apagado, 1 vermelho aceso, 2 âmbar apagado, 3 âmbar aceso).
5. **Renderer:** `LampModuleRenderer` passa a receber a cor, e os conjuntos `LENS` e
   `EMITTER` incluem os sprites âmbar.
6. **Cor vem da indicação:**
   - Alerta GPWS: vermelho;
   - Atenção GPWS e GPWS INOP: âmbar;
   - Função específica: âmbar apagada e, acesa, vermelho em warning e âmbar em caution;
   - Redstone Link: cor escolhida pelo jogador (futuro).
7. Validar no Blockbench e no jogo, com e sem shader. Os glitches com Complementary
   seguem em aberto (guia, seção 11).

Nenhuma tinta por vértice nem recolorir em código: as duas cores são texturas pintadas com
a rampa, como pede o guia.

## 7. Redstone Link (futuro, reservado)

- O modelo de dados do módulo ganha `indication = redstone_link`, um par de frequências e
  uma cor desde a fase 5. Ainda sem lógica.
- A tela mostra a opção "Redstone Link · em breve". O scroll a inclui no ciclo só quando
  ela for implementada.
- Implementação futura: receptor de Redstone Link do Create (como os sensores já fazem com
  `SensorOutputConfig.FrequencyPair`); acende com sinal > 0, a intensidade segue o sinal.

## 8. Fases

Cada fase termina com `.\gradlew.bat build`, testes e docs atualizados.

1. **Guia e decisões.** Seção de telas no guia visual (seção 4 deste plano); cores da
   lâmpada confirmadas na seção 9.4.
2. **Renomear.** `avionics_computer` com alias, lang, peripheral `avionics_computer`,
   `GpwsAssetsTest` → `AvionicsComputerAssetsTest`. Config antiga no NBT é ignorada.
3. **Motor.** Remover perfil, modo, fase manual e `GpwsRule`. Introduzir funções com
   estado (ativa/alerta/INOP/inibida), papéis de sensor, sensibilidade e os anunciadores
   `gpws.warning`/`gpws.caution`/`gpws.inop`. Reaproveitar `GpwsEngineTest` para os
   envelopes.
4. **Gateway CC.** `getStatus()`, `getSensors()` (todas as leituras com papel),
   `getAlerts()`, `test()`, `setInhibited(function, bool)` e `setSensitivity(value)`;
   eventos `avionics_alert_changed` e `avionics_sensors_changed`. Peripherals dos sensores
   e Bridge não mudam.
5. **Tela do computador e bloco.** Tela da seção 5.1 com atualização ao vivo; botão TEST
   físico; texturas de estado da tela do bloco.
6. **Mesa.** Indicações (com migração de canal/severidade → indicação), buzzer
   simplificado, clique de reconhecimento, painel geral enxuto, saída da Configuração
   básica e dos campos de teste. Lua: `configureAlertModule` vira atalho que mapeia para
   indicação; `setIndication(column, row, indication)` novo; `applyBasicSetup` fica
   obsoleto e apenas aplica os defaults.
7. **Lâmpada âmbar.** Seção 6: Blockbench, export, modelos, renderer,
   `LampModuleAssetsTest`.
8. **Configuração no mundo.** Scroll com a chave e rótulo flutuante (seção 5.4).
9. **Documentação final.** `gpws.md` vira `avionics-computer.md`; atualizar
   `modular-table.md`, `avionics-network.md`, PRD e README; arquivar o plano da fase 4.

As fases 2–4 podem sair juntas num mesmo PR (mudança de contrato). As fases 6–8 dependem
da 3 (anunciadores) mas não entre si, exceto 8 depender de 6.

## 9. Decisões finais (2026-10-28)

- O scroll exige a wrench do Create; não há atalho de mão vazia.
- O painel geral da mesa lista as lâmpadas acesas (seção 5.2).
- Avisos de altitude falados (modo 6: "500", "100", "minimums") ficam para depois deste
  refactor.
