# Módulos de lâmpada — emissão visual

Cada módulo de lâmpada tem estado visual independente. Em uso normal, seu canal GPWS e severidade mínima determinam se a lâmpada está apagada (0) ou acesa (15). A configuração básica responde a `gpws.any` desde Caution. O passe visual escala apenas o núcleo/lente da lâmpada; carcaça, mesa e módulos vizinhos não recebem esse brilho.

Para testar os níveis intermediários, o peripheral `modular_table` expõe `setLampTestIntensity(column, row, level)` e `clearLampTestIntensity(column, row)`. As coordenadas são 1-based e `level` aceita valores inteiros de 0 a 15, inclusive. O override é somente visual e temporário: não altera canal, threshold, sinal, nem grava no save. Ao limpar o override, a aparência volta a seguir o sinal e o threshold do módulo.

O programa [lamp-emission-test.lua](examples/lamp-emission-test.lua) percorre em ordem cada lâmpada 1×1 e 2×1 instalada. Para cada uma, aplica os níveis de 0 a 15, aguardando meio segundo em cada nível, e então passa para a próxima. Ele limpa os overrides ao terminar ou ao ser interrompido. O botão Test panel oferece também um teste integrado de três segundos para lâmpadas e buzzers.

Copie o arquivo para um computador CC:Tweaked conectado à mesa e execute:

```text
lamp-emission-test left
```

`left` pode ser substituído pelo lado ou nome do peripheral. O argumento pode ser omitido para selecionar a primeira mesa encontrada. Para ver a emissão visual com clareza, observe a mesa em uma área escura.

## Luz no ambiente

A mesa também emite luz de bloco do vanilla, que ilumina os blocos ao redor com ou sem shaders. O nível vem da lâmpada mais forte da mesa: `intensidade × lampLightLevel / 15`, arredondado. `lampLightLevel` fica na config do servidor do mod (padrão 5, um terço da tocha; `0` desliga). O override de teste também altera essa luz.

Como é luz de bloco comum, todo shaderpack a usa sem precisar de suporte específico. A cor, porém, depende do pack: a luz do vanilla não tem cor, e cada shader tinge os blocos que conhece pela própria tabela de IDs. Para a mesa, isso costuma resultar em luz branca ou quente, não vermelha.

## Aceite visual

1. Reinicie o cliente após a alteração Java; F3+T recarrega apenas recursos.
2. Execute o programa e confirme a progressão visual em cada lâmpada, de apagada até a intensidade máxima.
3. Verifique que somente a lâmpada sob teste muda e que as outras permanecem no estado de seus sinais.
4. Confirme que, ao terminar/interromper, cada lâmpada volta ao estado normal do respectivo sinal e threshold.
5. Com F3, confirme que o nível de luz ao lado da mesa acompanha a lâmpada mais forte e volta a 0 quando todas apagam.

A compilação verifica a integração. Aparência, transparência e progressão precisam ser observadas dentro do jogo.
