# Features — spec corrente

Registro das decisões de escopo tomadas na sessão de grilling, ref por ref. Alvo: Minecraft 26.2, Fabric. Features de comportamento são toggleáveis via config; receitas e worldgen são sempre ligados (ver ADR-0002).

Legenda: ✅ entra · ❌ fora · 🔧 entra com alteração · ❔ pendente.

## Princípios globais

- **Não duplicar vanilla 26.2:** receitas/mecânicas dos refs que já se tornaram vanilla na 26.2 **não** são reimplementadas (evita id de receita duplicado e trabalho redundante). Ex.: corte direto de deepslate e corte de cobre já são vanilla.
- **Sem filosofia de preço única:** o mod não força um balanço único. Cada feature preserva a intenção do seu ref (ex.: OmniCut = "preço vanilla"; More-Buttons = generoso). Conflitos de receita são resolvidos por cessão de propriedade, não por unificação de preço.
- **Toggle após reinicialização:** mudanças nos toggles de comportamento entram em vigor somente na próxima inicialização do jogo ou servidor, e a tela de configuração deve informar essa exigência. Em multiplayer, a configuração de gameplay do servidor é autoritativa.
- **Padrão habilitado:** todas as features de comportamento incluídas no escopo vêm habilitadas por padrão.

---

## cheaper-clocks

- 🔧 **Clock mais barato** — substitui a receita vanilla `minecraft:clock`.
  - Vanilla: 4 gold ingots + 1 redstone.
  - **Decisão:** **4 `gold_nugget` + 1 `redstone`**, formato diamante (nuggets nos 4 braços, redstone no centro), resultado 1 clock. Sem `gold_ingot`.
    ```
     #      # = gold_nugget (×4)
    #X#     X = redstone (×1)
     #
    ```

---

## cheaper-compasses

- 🔧 **Compass mais barato** — substitui a receita vanilla `minecraft:compass`. Espelho do clock.
  - Vanilla: 4 iron ingots + 1 redstone.
  - **Decisão:** **4 `iron_nugget` + 1 `redstone`**, formato diamante, resultado 1 compass. Sem `iron_ingot`.

---

## Village-Bell-Recipe

- ✅ **Sino craftável** — receita **aditiva** (vanilla não tem craft de bell). Ref já mira 26.2.
  - **Decisão:** receita como o ref, resultado 1 `minecraft:bell`.
    ```
    LLL     L = #minecraft:logs (×3, via tag)
    SGS     G = minecraft:gold_block (×1)
    S S     S = minecraft:stone (×4)
    ```

---

## More-Buttons

- ✅ **Botões via crafting (override)** — 1 bloco → **4** botões (vanilla = 1).
  - Madeira (×12): acacia, bamboo, birch, cherry, crimson, dark_oak, jungle, mangrove, oak, pale_oak, spruce, warped (1 planks → 4).
  - Pedra (×2): stone, polished_blackstone (1 bloco → 4).
- ✅ **Botões via stonecutting (aditivo)** — 1 bloco → **6** botões.
  - stone → 6 stone_button; polished_blackstone → 6 polished_blackstone_button.
- **Decisão:** entra tudo; crafting = 4/bloco; stonecutter = 6/bloco (versão generosa; o mod **não** adota filosofia "sem bônus" única).
- ✅ **Resolvido — dono dos botões de pedra no stonecutter:** More-Buttons registra `stone_button` e `polished_blackstone_button` via stonecutter (count 6). OmniCut **não** registra essas duas (cedidas aqui).

---

## Recycling

Recuperação de material base a partir de gear. **Output sempre 1 unidade** (recuperação parcial — não dá dupe). Sobrescreve o comportamento vanilla (ferro/ouro dão nugget na vanilla; aqui dão ingot inteiro — buff deliberado).

- ✅ **Reciclagem na fornalha** (smelting + blasting) — 6 materiais:
  - copper → 1 copper_ingot · chainmail → 1 iron_ingot · iron → 1 iron_ingot · golden → 1 gold_ingot · diamond → 1 diamond · netherite → 1 netherite_scrap.
  - Aceita todo o gear do material: helmet/chestplate/leggings/boots + sword/pickaxe/axe/shovel/hoe + horse_armor + nautilus_armor + spear.
- ✅ **Reciclagem na crafting table** — 3 materiais (não vão à fornalha):
  - leather (4 peças de armadura + `leather_horse_armor`) → 1 leather.
  - turtle (`turtle_helmet`) → 1 turtle_scute.
  - armadillo (`wolf_armor`) → 1 armadillo_scute.
- ✅ **Resolvido — itens conferidos no registry da 26.2:** `copper_*`, `<material>_nautilus_armor` e `<material>_spear` existem para copper/iron/golden/diamond/netherite; chainmail só tem as 4 peças de armadura. A vanilla já recicla copper/iron/gold por um único id por método (`copper_nugget_from_smelting`, etc.), então o override reusa esses ids — é por isso que chainmail viaja dentro da receita do ferro.

---

## OmniCut

Woodcutting / stonecutting / recycling. Preço "vanilla" preservado (calibragem pré-calculada do ref). ~409 receitas no ref; na 26.2 reimplementamos só o que **não** é vanilla. Pacote **completo**.

- ✅ **Wood cutting** (stonecutter) — log/wood → 15 produtos: stairs(×4), slab(×8), fence, fence_gate, door, trapdoor, sign, boat, planks, stick, wood, strip_log, strip_wood, pressure_plate, button(×4). 11 tipos de madeira. Aceita log/stripped_log/wood/stripped_wood como input.
- ✅ **Wood recycling** (stonecutter) — produtos de madeira → planks/sticks.
- ✅ **Copper cutting** (stonecutter) — 40 receitas: bloco de cobre do estado → `copper_bars` (24), `copper_chain` (8), `lightning_rod` (3), `copper_door` (5) e `copper_trapdoor` (4), nos 8 estados de oxidação. São as cinco famílias que a vanilla 26.2 **não** corta; `cut_copper`/stairs/slab/chiseled/grate ficam com a vanilla. Nenhum desses produtos é reciclável no RootBoot nem na vanilla (os únicos consumidores vanilla aplicam cera), então nada disso realimenta material.
- ⚠️ **"Preço vanilla" do cobre não é custo exato:** medidos contra a bancada, os rendimentos do ref divergem — barras (24 = 9 lingotes) e haste (3 = 9 lingotes) empatam, corrente (8 = 9,78) e porta (5 = 10) saem levemente baratas, e o **alçapão (4 = 16 lingotes) sai 78% mais barato**. É a calibragem do ref, mantida verbatim por decisão da issue #32; o desconto é intencional e não forma ciclo.
- ✅ **Copper recycling** (stonecutter uncut + bancada unslab) — 8 estados de oxidação × (stairs/chiseled/grate → `cut_copper`) + (2 slabs → `cut_copper`).
- ✅ **Resolvido — Rock cutting não sobrou nada:** o ref declara seus overlays por `pack_format`, e na 26.2 (formato 107) o `1.21.2-1.21.11` (máximo 94) não se aplica — junto com ele saem os cortes diretos de deepslate e o `polished_blackstone_button`. Os três overlays aplicáveis são `1.21.2-current`, `1.21.9-current` e `26.2-current`; a camada base do pack continua existindo, mas seus arquivos estão sob `data/meenimc/recipes/` (plural), pasta que versões modernas não leem, então seu conteúdo é ignorado. O único `rock/cut` que a 26.2 ainda lê é `stone_button`, cedido ao More-Buttons. **Zero receitas de rock cutting.**
- ✅ **Stone recycling** (stonecutter uncut) — 30 receitas: produtos de pedra → bloco base (inclui smooth/cracked → base). `deepslate → cobbled_deepslate` foi removido da lista porque a vanilla já corta. Pressure plates devolvem 2; todo o resto devolve 1.
- ✅ **Slab reassembly** (unslab, bancada) — 2 slabs lado a lado → bloco. Rock (44) + copper (8) + wood.
- ✅ **Pedras novas 26.2** — cinnabar e sulfur entram pelo overlay `26.2-current` do ref, que define uncut (2) + unslab (6); não há `cut` para elas no ref, e a vanilla já corta as duas famílias.
- ⚠️ **Divergência do ref — botão não recicla:** o ref corta botão 1:1 e por isso pode reciclá-lo de volta. O RootBoot é generoso (4 na bancada, 6 no stonecutter), então devolver material a partir de um botão multiplicaria recurso sem limite. Nenhuma receita do RootBoot consome botão — nem a de pedra (`stone_button`/`polished_blackstone_button`), nem a de madeira, que perdeu o botão das recuperações de `planks`/`stairs`/`slab`/`stick`.
- **Notas:** reciclagem é só stonecutter (README menciona bancada, mas os dados não têm). Botões de madeira do OmniCut (stonecutter, log→4) coexistem com os do More-Buttons (bancada, planks→4) — ids/tipos distintos, sem conflito.

---

## better-trees

Melhorias estéticas nas árvores do overworld. Sobrescreve ~40 `configured_feature` vanilla (oak, birch, cherry, mangrove, mega_spruce, variantes `_bees`, etc.). Baseline = conjunto do **overlay 26.1** (inclui pale_oak + variantes `_leaf_litter`).

- ✅ **Árvores melhoradas** — reimplementado via Fabric datagen (Java → JSON que sobrescreve os ids vanilla).
- **Toggle:** ❌ **não toggleável — sempre ligado** quando o mod carrega. **Exceção ao ADR-0002** (worldgen resolve antes de config runtime; por decisão, não expomos como datapack opcional).
- Afeta apenas chunks novos.
- ❌ `cleartrees.mcfunction` (utilitário admin do autor) — **descartado**.

---

## Wool-Tweaks

Reduzido a uma única receita.

- ✅ **`wool → 4 string`** — shapeless, 1 `#minecraft:wool` (qualquer cor, via tag) → 4 string. Aditivo.
- ❌ **Recolorir por clique-direito** (wool/bed/carpet) — **fora**.
- ❌ **Re-tingir na grade** (48 receitas wool/bed/carpet) — **fora**.
- Nota: sem o clique-direito, não há dependência de event handler nem do Collective; é só receita.

---

## special-boots

Dois encantamentos de bota (single-level, slot feet, `supported_items: #minecraft:enchantable/foot_armor`, weight 5, custo 15–45, anvil_cost 2). Config própria absorvida na Cloth unificada.

- **Compatibilidade multiplayer:** Heavyfoot e Lightfoot são exceções à meta de cliente vanilla-compatible; seu uso e apresentação corretos exigem RootBoot no cliente.
- **Exclusividade:** Heavyfoot e Lightfoot são mutuamente exclusivos e não podem coexistir na mesma bota, divergindo deliberadamente da referência original.
- **Desativação:** desabilitar Heavyfoot ou Lightfoot suspende somente seu efeito; encantamentos já presentes nas botas permanecem intactos e voltam a funcionar após a feature ser reabilitada e o jogo ou servidor reiniciado.

- ✅ **Heavyfoot** — `EnchantmentEntityEffect` custom em `minecraft:tick`: num raio ao redor do player, converte grass/dirt/coarse_dirt/podzol/mycelium/rooted_dirt → `dirt_path` (abaixo) e destrói flores (`#minecraft:flowers`)/short_grass/tall_grass/fern/large_fern/dead_bush/sweet_berry_bush (pés e cabeça).
  - **Raio default = 1**; range **0–2** na config. Preserva literalmente a geometria da referência: 0 afeta uma área 1×1, 1 afeta 3×3 e 2 afeta 5×5.
- ✅ **Lightfoot** — mixin em `FarmlandBlock.fallOn`→`turnToDirt`: impede pisoteio de farmland se a bota tiver o encantamento. Na 26.2 a classe é `FarmlandBlock`, e `fallOn` chama `turnToDirt` uma única vez antes de delegar ao `super.fallOn`, então envolver essa chamada suspende apenas a conversão e preserva o restante da queda. Usa `@WrapOperation` (MixinExtras, embutido no Fabric Loader) em vez do `@Redirect` originalmente previsto, porque um redirect reivindica o call site com exclusividade e quebraria outros mods que alterem pisoteio de farmland.
- **Obtenção (opção A):** survival via **mesa de encantar + bigorna** — adicionar às tags `#minecraft:in_enchanting_table` e `non_treasure`. Sem trades, sem loot/treasure.

---

## Vanilla-Refresh (subconjunto curado)

Só as 18 features abaixo. Reimplementação em Java (os refs são `.mcfunction`; a mecânica exata de cada uma será lida das funções na implementação). `(C)` = client-side, `(S)` = server/gameplay.

**Entram:**
- ✅ #3 **Daycounter** `(C)` — exibe na action bar o dia global do Overworld no início de cada dia e em toda entrada ou reconexão do jogador; alcança jogadores em qualquer dimensão, sem contagens independentes. O anúncio comum monta “— DAY N —”, com texto traduzível, e permanece plenamente visível por cerca de 2 segundos. A animação especial dura aproximadamente 11 segundos, com cores e sons, e ocorre somente na transição para dias múltiplos de 100; reconexões durante o mesmo dia recebem o anúncio normal.
- ✅ #4 **Homing Experience Orbs** `(S)` — após 20 ticks, cada orb persegue dinamicamente e através de blocos o jogador não-espectador mais próximo em até 64 blocos; move-se a 0,3 bloco/tick nos primeiros 10 ticks de perseguição e a 0,6 bloco/tick depois, descartando comandos duplicados da referência que dobravam essas velocidades. Sem alvo válido nesse alcance, permanece imóvel. Aplica-se uniformemente em todas as dimensões e locais, sem a exceção da referência para o portal de saída do End.
- ✅ #6 **Crops XP** `(S)` — uma única rolagem tem chance de gerar exatamente um orb de 1 ponto de XP ao colher uma plantação completamente madura sem Silk Touch; plantas imaturas e colheitas com Silk Touch nunca concedem a recompensa. Fortuna não altera a chance nem a quantidade. Chance de 75% para trigo, cenoura, batata, beterraba, cacau e fungo do Nether; 100% para melão e abóbora. Esses valores divergem deliberadamente tanto dos 20%/40% documentados quanto dos 50%/100% executados pelo Vanilla Refresh.
- ✅ #7 **Jukebox Music Override** `(C)` — enquanto existe ao menos um jukebox com disco em reprodução dentro do alcance audível do cliente, a música ambiente fica suprimida e nenhuma nova faixa começa, independentemente do volume configurado pelo usuário para a categoria sonora do jukebox. Ao deixar de haver reprodução ativa no alcance, o agendador vanilla volta a funcionar normalmente, sem retomar do ponto anterior a faixa interrompida. Isso substitui o comportamento pontual da referência, que interrompia a música apenas na inserção do disco. Na 26.2 o cliente só sabe que um jukebox toca pelos level events 1010/1011, porque o `JukeboxBlockEntity` não sincroniza a música; o RootBoot registra cada início com o tick de término derivado de `JukeboxSong.lengthInTicks`, para que um disco cujo evento de parada nunca chegou expire em vez de suprimir a música para sempre. A supressão cancela `MusicManager.tick`, congelando o contador da próxima faixa em vez de silenciá-la — é isso que a torna independente do volume da categoria; o alcance é medido a partir do listener de áudio (a câmera), não do jogador. **Limite conhecido:** os eventos 1010/1011 são emitidos uma única vez e alcançam só quem está a 64 blocos, então um disco iniciado enquanto o cliente estava fora desse raio não suprime a música — em vanilla ele também não é ouvido nesse caso, de forma que "alcance audível do cliente" é exatamente o conjunto que o RootBoot observa.
- ✅ #8 **Extra Loyal Tridents** `(S)` — um tridente com Lealdade é impedido de ser destruído ao entrar no vazio. Se o dono estiver morto, desconectado ou em outra dimensão, o tridente permanece persistente até que o retorno vanilla possa continuar com o dono na mesma dimensão; não teleporta entre dimensões nem é inserido diretamente no inventário.
- 🔧 #9 **Half-health Babies** `(S)` — ampliada para **qualquer entidade viva reconhecida pelo Minecraft como bebê**, passiva, neutra ou hostil, inclusive variantes futuras compatíveis. Enquanto bebê, possui exatamente 50% da vida máxima efetiva que teria no ambiente atual sem esta feature, incluindo modificadores fornecidos por outros mods; a redução é multiplicativa, não a substituição por um valor vanilla fixo. A vida atual preserva sua proporção ao entrar ou sair do estado bebê; por exemplo, 6/10 torna-se 12/20 ao crescer. Ao desabilitar a feature, bebês existentes recuperam a vida máxima sem a redução do RootBoot conforme são carregados, preservando a mesma proporção. Sprint-jump e one-shot (crítico diamante/netherite) **fora**.
- ✅ #10 **Improved/Local Death Sound** `(C)` — som de morte **por-causa, local** (só o jogador morto ouve). Damage types sem mapeamento específico, inclusive os adicionados por outros mods, usam um som de morte genérico local. **Sem** sub-opção de "som fixo". A causa vem do `ClientboundDamageEventPacket`, único ponto em que o cliente conhece o damage type — o evento de morte que o segue não o carrega —, e só é atribuída à morte se tiver chegado no mesmo tick ou no anterior, tolerância mínima porque os dois pacotes do mesmo tick do servidor podem ser processados em lados opostos de um tick do cliente. O som vanilla de morte do jogador é preservado; esta feature acrescenta o som local por causa.
- ✅ #12 **Drop Ladder** `(S)` — preserva o comportamento da referência: enquanto está numa coluna de escadas e segura uma escada em qualquer mão, cada novo pressionamento de agachar coloca exatamente uma unidade abaixo da extremidade inferior. Consome uma escada fora do criativo; se houver escadas nas duas mãos, consome primeiro da mão principal e usa a secundária somente como fallback. Espaço obstruído não coloca nem consome. Escadas penduradas dependem da cadeia acima para permanecer sustentadas.
- ✅ #13 **Totem Works In Void** `(S)` — ao receber dano `out_of_world` em qualquer dimensão vanilla ou modded com um totem em qualquer mão, ativa e consome um totem pelo fluxo vanilla. Por até 60 segundos, o jogador sobe normalmente e desce lentamente enquanto segura agachar. O resgate termina imediatamente ao pousar ou quando o tempo expira; o término encerra apenas o resgate atual, e um novo evento letal de dano `out_of_world` pode consumir outro totem e iniciar um novo prazo completo.
- ✅ #14 **Player Head Drop** `(S)` — toda morte solta uma cabeça com o perfil do jogador separadamente no local da morte, inclusive com Soul Links ou `keepInventory` ativos. A cabeça nunca é capturada pela Soul; sua entidade de item original é invulnerável e não sofre despawn até ser coletada. Ao entrar em um inventário, a proteção termina, e descartá-la novamente produz uma entidade sujeita a dano e despawn vanilla. Em morte no void, permanece na mesma dimensão e X/Z, sobre o bloco sólido mais alto da coluna ou em Y=96 se a coluna estiver vazia. Desabilitar a feature impede novos drops, mas preserva a proteção das entidades originais ainda existentes.
- ✅ #15 **Echo Shard Silence** `(S)` — um echo shard caído a até 1 bloco de um mob elegível é consumido, silencia-o permanentemente e impede seu despawn natural. Se um shard estiver no alcance de vários mobs elegíveis, vincula-se somente ao mais próximo; empates exatos usam uma ordem estável baseada no UUID. Água desfaz o vínculo e solta um echo shard no local. Quando o mob morre, exatamente um echo shard integra seus drops no local, independentemente da causa ou de haver jogador responsável; nunca é inserido diretamente no inventário. Jogadores, Ender Dragon, entidades não-vivas e mobs já silenciosos são inelegíveis. Desabilitar a feature impede apenas novos vínculos; mobs já vinculados continuam silenciosos e persistentes, e água ou morte ainda devolvem o shard.
- ✅ #16 **Time Offset** `(S)` — transformação única avaliada na primeira inicialização do mundo: quando habilitada, acrescenta um dia para que o mundo comece no dia 1. Alterações posteriores no toggle não aplicam nem revertem o deslocamento; a configuração deve informar que a opção afeta apenas mundos criados posteriormente.
- 🔧 #19 **Level Up Animation** `(C)` — **apenas level up** (splash na água **fora**). Uma única animação local, visível somente para o próprio jogador, dispara quando um aumento de nível cruza ao menos um múltiplo de 5; reduções nunca disparam e saltos grandes não empilham animações. No login ou na primeira inicialização após habilitar a feature, o nível atual é registrado silenciosamente como baseline e não gera animação. Isso corrige a referência, que exige que o nível final seja múltiplo de 5 e perde marcos atravessados.
- ✅ #21 **Subtitles on Major Events** `(C)` — título na primeira entrada de cada jogador no End, incluindo login ou reconexão já dentro da dimensão quando a descoberta ainda não está registrada, e na primeira vez que chega a até 64 blocos de qualquer Wither, incluindo login ou reconexão já dentro desse raio, independentemente de quem o invocou. Ambas as descobertas são persistentes por jogador e não se repetem. Eventos ocorridos enquanto a feature está desabilitada não são registrados; após habilitá-la, o primeiro evento elegível posterior exibe o título e cria o registro.
- ✅ #22 **Subtitles on Biome Discovery** `(C)` — cada jogador mantém um conjunto persistente de IDs de bioma descobertos. Na primeira entrada em qualquer bioma registrado, vanilla ou modded, recebe um título com seu nome traduzido ou com o ID formatado como fallback. O bioma atual também é verificado no login ou reconexão e anunciado se ainda não estiver registrado. O mesmo bioma não é anunciado novamente para aquele jogador. Biomas visitados enquanto a feature está desabilitada não são registrados; depois de habilitada, a primeira entrada em cada bioma gera o anúncio mesmo que ele já tenha sido visitado durante a desativação. Isso substitui as categorias manuais da referência.
- ✅ #23 **Trimmed Armored Piglins** `(S)` — somente piglins comuns que já nascem com armadura são elegíveis; piglin brutes ficam fora. Cada peça equipada recebe uma rolagem independente de 25% fora de bastions ou 50% dentro, permitindo várias peças com trim. Esse algoritmo produz aproximadamente os 8%/16% gerais anunciados pela referência. Cada peça selecionada escolhe uniformemente entre sete combinações: Rib com diamond/iron/netherite/gold ou Snout com diamond/netherite/gold. Leggings também incluem Silence com gold como oitava opção de mesmo peso. A rolagem ocorre uma única vez no spawn; habilitar a feature não processa nem rerrola piglins existentes. Essa distribuição substitui as rolagens sequenciais enviesadas da referência.
- ✅ #27 **Low Health Sound** `(C)` — uma batida é ouvida somente pelo jogador ferido quando um dano não fatal reduz sua vida real e o deixa com até 8 pontos (4 corações). Corações de absorção não aumentam o limiar; um golpe absorvido integralmente, sem redução da vida real, não dispara. O som ocorre uma vez por evento de dano, sem cooldown adicional. O limiar diverge dos 5 pontos da referência. A decisão é tomada em `LocalPlayer.hurtTo`, por onde passa toda mudança de vida vinda do servidor e onde a vida anterior ainda está disponível; é o mesmo ponto em que o vanilla decide que o jogador foi ferido. **Limite conhecido:** o gatilho é a queda de vida sincronizada, não o evento de dano em si, então uma redução por comando dispara a batida e dois danos consolidados numa única atualização de vida produzem uma só.
- ✅ #30 **Stands & Frames Invisibility** `(S)` — uma poção splash de invisibilidade oculta persistentemente todos os armor stands, item frames e glow item frames alcançados pela explosão vanilla. Água splash reverte somente a invisibilidade aplicada pelo RootBoot; entidades que já eram invisíveis por comando ou outro mod não são alteradas. Desabilitar a feature impede novas aplicações, mas displays já ocultos permanecem invisíveis e continuam aceitando reversão por água.

**Do grupo "disabled by default" do VR — também entram habilitadas por padrão aqui:**
- ✅ A **Death items don't despawn** `(S)` — itens dropados na morte não desaparecem por tempo enquanto a feature está habilitada, mas continuam vulneráveis a lava, fogo, explosões, cactos, vazio e demais perigos conforme as regras vanilla. Itens excedentes liberados na recuperação de uma Soul seguem o estado desta feature: ficam protegidos contra despawn quando ela está habilitada e usam o prazo vanilla quando está desabilitada. Após reiniciar com a feature desabilitada, itens anteriormente protegidos voltam ao comportamento vanilla com um novo prazo completo de despawn; novos drops não recebem proteção.
- ✅ B **Soul Links** `(S)` — ao morrer, os itens e o XP que a morte vanilla efetivamente descartaria viram uma "Soul"; somente o proprietário fora do modo espectador pode ativá-la com clique direito/ação de usar para recuperar 80% dos pontos totais exatos de experiência, incluindo o progresso parcial do nível, com arredondamento para baixo e crédito direto ao jogador, sem gerar orbs; clique esquerdo, colisão ou proximidade não ativam a recuperação. Isso diverge da referência, que calcula sobre níveis inteiros, limita a conversão a partir do nível 35 e gera um orb. Na recuperação, o RootBoot tenta inserir os itens diretamente no inventário do jogador e libera apenas o excedente no local da Soul, também divergindo da referência, que libera todo o conteúdo no mundo. A Soul é visível a todos, imóvel e invulnerável a jogadores e ao ambiente. Quando um operador remove sua entidade visual por comando, o estado persistente é finalizado como numa expiração, liberando itens e 80% do XP no local ou deixando a materialização pendente até o chunk carregar; o conteúdo não é apagado nem fica órfão. Quando formada no void, permanece na mesma dimensão e nas mesmas coordenadas X/Z da morte, sobre o bloco sólido mais alto da coluna; se a coluna estiver vazia, aparece em Y=96. Cada morte com alguma perda cria uma Soul independente e persistente; um jogador pode manter várias delas, sem substituição ou fusão. Sua duração global possui dois modos explícitos: permanência ilimitada, usada por padrão, ou prazo em minutos inteiros a partir de 1; zero é inválido. A política atual se aplica dinamicamente a todas as Souls existentes. Ao reiniciar com um prazo reduzido, Souls cujo tempo acumulado já alcançou o novo limite expiram imediatamente e liberam seus itens e experiência. No modo ilimitado, o tempo já acumulado permanece salvo e congelado; se um prazo voltar a ser configurado, a contagem retoma desse valor. O prazo avança enquanto o proprietário está online, mesmo com a Soul ou seu chunk descarregados, e pausa enquanto ele está offline. Ao expirar, libera todos os itens no local e materializa em orbs vanilla os mesmos 80% dos pontos exatos de experiência, com arredondamento para baixo, que seriam recuperados diretamente; os itens seguem o estado de Death items don't despawn. Se o chunk estiver descarregado, a Soul expira imediatamente no estado persistente e materializa os drops uma única vez quando o chunk voltar a carregar, sem forçá-lo. Desabilitar a feature impede somente a criação de novas Souls; as existentes continuam carregando, protegidas e recuperáveis. Sua representação usa apenas tipos de entidade vanilla e estado autoritativo no servidor, sem entidade customizada, para permitir clientes sem RootBoot (ADR-0006). Com `keepInventory=true`, nenhuma Soul é criada e a feature não altera itens nem XP. Esta é uma divergência deliberada do Vanilla Refresh, que depende de `keepInventory=true` e oferece ativar a gamerule. O RootBoot não altera nem usa essa gamerule para formar a Soul (ADR-0005). *(complexo)*
- ✅ D **Stop Music on Death** `(C)` — ao morrer, interrompe somente a música ambiente local do jogador morto. Jukeboxes e demais sons continuam. Parar o `MusicManager` alcança apenas a faixa ambiente que ele mantém, o que preserva as outras categorias por construção.
- ✅ J **Death Stats** `(S)` — a cada morte, publica para todos os jogadores online uma mensagem traduzível com o nome do jogador, o tempo sobrevivido e sua contagem total obtida da estatística vanilla persistida, inclusive mortes anteriores à instalação do RootBoot. O tempo avança somente enquanto o jogador está conectado e vivo, pausa offline, continua na reconexão e zera após cada morte; antes da primeira morte, começa na primeira entrada.
- ⚠️ **Interação A × B:** ambos protegem itens na morte. Default: **Soul Links (B) tem precedência** — com B ligada, os drops vão para a Soul e A não se aplica; A rege quando B está desligada. (Revisar na implementação.)

**Fora:** #1 Player Sitting, #2 Mob Health Display, #5 Craft Sounds, #11 Better Armor Stands, #17 Wither Head Drop, #18 Equipable Banners, #20 Improved Block Animations, #24 Global Death Sound, #25 Spectator Ghost, #26 Griefing Gamerules, #28 Better Lodestones (N/A na 26.2), #29 Path Sprinting, #31 Recovery Coordinates, #32 Readable Clocks, #33 Party Cake, #34 Improved Spectator, #35 Wands, #36 Item Sets, #37 Admin Commands, #38 Join/Exit Sounds, #39 Player Statistics, #40 Viewable Gamerules, #41 Playerlist. Disabled-by-default fora: C First Join Chat, E Enderdragon Drops Elytra, F Renewable Dragon Eggs, G Looping Jukebox (obsoleto/vanilla), H Gravestones, I Tips, K Dynamic Optifine Lighting (buggy), L Exploding Blast Furnace (piada).
