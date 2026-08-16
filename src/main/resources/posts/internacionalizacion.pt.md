---
title: "Internacionalização (i18n)"
summary: "Como preparar uma aplicação para falar vários idiomas: arquivos de tradução, detecção de idioma e textos dinâmicos sem duplicar código."
---

Esta semana o curso nos levou a um problema bem real: nossa aplicação só falava espanhol, mas os
usuários não. A **internacionalização** —abreviada *i18n*, porque há 18 letras entre o "i" e o
"n"— é a prática de projetar o software para que suporte vários idiomas sem reescrever o código.

## Que problema isso resolve?

Sem i18n, cada texto está cravado no código. Se o botão diz `Iniciar sesión` e queremos a versão
em inglês, seria preciso duplicar a tela inteira: dois arquivos fazendo o mesmo trabalho e que, a
partir daquele dia, precisam ser corrigidos duas vezes.

Com i18n o código não contém frases, e sim **chaves**, e as frases moram em arquivos de tradução,
um por idioma.

```json
// messages/es.json
{ "nav.login": "Iniciar sesión" }

// messages/en.json
{ "nav.login": "Sign in" }
```

```tsx
// Na view, no lugar do texto vai a chave:
<span>{t["nav.login"]}</span>
```

A tela volta a ser uma só. Acrescentar um idioma passa a ser acrescentar um arquivo.

## Conceitos que vimos

- **Arquivos de recursos**: um JSON —ou um `.properties`, conforme a plataforma— por idioma, com
  pares chave–texto.
- **Detecção de idioma**: pode vir do navegador, da URL ou da preferência salva do usuário. Cada
  opção tem consequências diferentes e merece uma escolha consciente.
- **Interpolação**: as traduções aceitam variáveis, por exemplo `"Olá, {nome}"`. A ordem das
  palavras muda entre idiomas, então emendar pedaços soltos não funciona.
- **Localização (l10n)**: vai além do texto. Datas, moedas, números e até a ordem alfabética mudam
  conforme a região.

## Como aplicamos no projeto

O idioma vive **na URL**: `/es`, `/en` e `/pt`. Isso é uma decisão, não um detalhe. Um endereço
com o idioma dentro pode ser compartilhado, pode ir para os favoritos e o botão "voltar"
funciona; se o idioma morasse apenas na memória do navegador, duas pessoas abrindo o mesmo link
veriam coisas diferentes.

Os três dicionários são arquivos planos, e o espanhol é o de referência:

```ts
// O bundle espanhol tipa os outros dois: se en.json ou pt.json
// perderem uma chave, o projeto não compila.
export type Dictionary = typeof es;
export type MessageKey = keyof Dictionary;
```

Foi esse detalhe que mais nos poupou. Uma tradução incompleta deixa de ser algo que alguém
descobre em produção e vira um erro de compilação.

Da localização cuida o próprio navegador, através do `Intl`, sem biblioteca extra:

```ts
new Intl.DateTimeFormat(locale, { day: "2-digit", month: "short", year: "numeric" });
new Intl.NumberFormat(locale, { style: "currency", currency: "USD" });
```

A mesma data se lê `18 jul 2026` em espanhol e `Jul 18, 2026` em inglês; o mesmo preço se escreve
`$1,999` ou `1999 US$`, conforme onde cada idioma coloca o símbolo.

No backend seguimos o caminho oposto, também de propósito: **a API não traduz**. Quando algo
falha, ela responde um código estável —`BAD_CREDENTIALS`, `EMAIL_TAKEN`— e é o front que o
transforma em uma frase. Assim o idioma é decidido em um único lugar, aquele que tem o
dicionário. A única exceção são os e-mails: a mensagem de recuperação de senha é escrita e
enviada pelo backend, então as três versões dela moram lá, e quem pede o link diz em que idioma o
quer.

> **Lição aprendida:** vale definir as chaves desde o primeiro dia. Migrar textos cravados para
> chaves depois é trabalho dobrado, e o momento em que dói é justamente quando já há telas
> suficientes para a tarefa dar preguiça.

## Próximos passos

Estender as traduções à área privada e fazer com que esta própria bitácora deixe de ser estática:
as entradas passarão para o banco de dados com uma linha por idioma, de modo que o conteúdo —não
só os botões— viaje traduzido.
