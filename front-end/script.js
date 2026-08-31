"use strict";

/* ============================================================
   Estado da aplicação (fica só em memória, na aba aberta)
   ============================================================ */
let itens = [];
let tipoSelecionado = "PERDIDO";
let filtros = { tipo: "", categoria: "", localizacao: "" };
let idEmEdicao = null; // null = modo cadastro | id do item = modo edição

const CATEGORIAS = [
  "Eletrônico", "Documento", "Chave", "Carteira/Bolsa", "Roupa/Acessório",
  "Óculos", "Animal de estimação", "Brinquedo", "Joia/Bijuteria", "Outro",
];

const STOPWORDS = new Set([
  "de", "da", "do", "das", "dos", "a", "o", "as", "os", "um", "uma", "uns", "umas",
  "e", "ou", "com", "sem", "para", "por", "em", "no", "na", "nos", "nas",
  "que", "se", "meu", "minha", "meus", "minhas", "seu", "sua",
]);

const PESO_CATEGORIA = 0.30;
const PESO_LOCALIZACAO = 0.25;
const PESO_DESCRICAO = 0.25;
const PESO_CARACTERISTICAS = 0.20;
const LIMIAR_MINIMO = 20.0;

/* ============================================================
   Algoritmo de matching (mesma lógica em qualquer linguagem:
   Java, JS... só muda a sintaxe)
   ============================================================ */

function normalizar(s) {
  return (s || "").trim().toLowerCase();
}

function tokenizar(texto) {
  if (!texto || !texto.trim()) return new Set();
  const limpo = normalizar(texto).replace(/[^a-z0-9áàâãéèêíïóôõöúçñ\s]/g, " ");
  const tokens = new Set();
  for (const palavra of limpo.split(/\s+/)) {
    if (palavra.length >= 3 && !STOPWORDS.has(palavra)) tokens.add(palavra);
  }
  return tokens;
}

function jaccard(setA, setB) {
  if (setA.size === 0 || setB.size === 0) return 0;
  let intersecao = 0;
  for (const v of setA) if (setB.has(v)) intersecao++;
  const uniao = new Set([...setA, ...setB]).size;
  return uniao === 0 ? 0 : intersecao / uniao;
}

function compararCategoria(a, b) {
  if (!a || !b) return 0;
  return normalizar(a) === normalizar(b) ? 1 : 0;
}

function compararTexto(a, b) {
  return jaccard(tokenizar(a), tokenizar(b));
}

function compararListas(a, b) {
  if (!a || !b || a.length === 0 || b.length === 0) return 0;
  const setA = new Set(a.map(normalizar));
  const setB = new Set(b.map(normalizar));
  return jaccard(setA, setB);
}

function round1(v) {
  return Math.round(v * 10) / 10;
}

/** Retorna as correspondências de um item, ordenadas da maior pra menor compatibilidade. */
function encontrarCorrespondencias(referencia) {
  const tipoOposto = referencia.tipo === "PERDIDO" ? "ENCONTRADO" : "PERDIDO";
  const resultado = [];

  for (const candidato of itens) {
    if (candidato.tipo !== tipoOposto) continue;
    if (candidato.resolvido || referencia.resolvido) continue;
    if (candidato.id === referencia.id) continue;

    const scoreCategoria = compararCategoria(referencia.categoria, candidato.categoria);
    const scoreLocalizacao = compararTexto(referencia.localizacao, candidato.localizacao);
    const scoreDescricao = compararTexto(referencia.descricao, candidato.descricao);
    const scoreCaracteristicas = compararListas(referencia.caracteristicas, candidato.caracteristicas);

    const detalhes = {
      categoria: round1(scoreCategoria * 100),
      localizacao: round1(scoreLocalizacao * 100),
      descricao: round1(scoreDescricao * 100),
      caracteristicas: round1(scoreCaracteristicas * 100),
    };

    const total = scoreCategoria * PESO_CATEGORIA
      + scoreLocalizacao * PESO_LOCALIZACAO
      + scoreDescricao * PESO_DESCRICAO
      + scoreCaracteristicas * PESO_CARACTERISTICAS;

    const compatibilidade = round1(total * 100);

    if (compatibilidade >= LIMIAR_MINIMO) {
      resultado.push({ item: candidato, compatibilidade, detalhes });
    }
  }

  resultado.sort((a, b) => b.compatibilidade - a.compatibilidade);
  return resultado;
}

function montarSugestaoContato(referencia, encontrado, compat) {
  const outraPessoa = referencia.tipo === "PERDIDO" ? encontrado.contatoNome : referencia.contatoNome;
  const telefoneOutro = referencia.tipo === "PERDIDO" ? encontrado.contatoTelefone : referencia.contatoTelefone;

  if (compat >= 70) {
    return `Essa é uma correspondência forte! Entre em contato com ${outraPessoa} pelo telefone ${telefoneOutro}.`;
  } else if (compat >= 40) {
    return `Possível correspondência com ${outraPessoa} (${telefoneOutro}). Confirme os detalhes antes de combinar a devolução.`;
  }
  return `Correspondência fraca com ${outraPessoa} — vale checar, mas confira bem as características.`;
}

/* ============================================================
   Cadastro / edição / listagem / ações
   ============================================================ */

function gerarId() {
  return Math.random().toString(16).slice(2, 10);
}

function cadastrarItem(dados) {
  const item = {
    id: gerarId(),
    tipo: dados.tipo,
    categoria: dados.categoria,
    descricao: dados.descricao.trim(),
    localizacao: dados.localizacao.trim(),
    caracteristicas: dados.caracteristicas,
    contatoNome: dados.contatoNome.trim(),
    contatoTelefone: dados.contatoTelefone.trim(),
    criadoEm: new Date().toISOString(),
    resolvido: false,
  };
  itens.unshift(item);
  return item;
}

/** Atualiza um item existente em memória, preservando id, criadoEm e resolvido. */
function atualizarItem(id, dados) {
  const item = itens.find((i) => i.id === id);
  if (!item) return null;
  item.tipo = dados.tipo;
  item.categoria = dados.categoria;
  item.descricao = dados.descricao.trim();
  item.localizacao = dados.localizacao.trim();
  item.caracteristicas = dados.caracteristicas;
  item.contatoNome = dados.contatoNome.trim();
  item.contatoTelefone = dados.contatoTelefone.trim();
  return item;
}

function listarItensFiltrados() {
  return itens.filter((item) => {
    if (filtros.tipo && item.tipo !== filtros.tipo) return false;
    if (filtros.categoria && item.categoria !== filtros.categoria) return false;
    if (filtros.localizacao &&
      !item.localizacao.toLowerCase().includes(filtros.localizacao.toLowerCase())) return false;
    return true;
  });
}

/* ============================================================
   Renderização (DOM)
   ============================================================ */

function preencherSelectsDeCategoria() {
  const campoCategoria = document.getElementById("campoCategoria");
  const filtroCategoria = document.getElementById("filtroCategoria");

  campoCategoria.innerHTML = '<option value="" disabled selected>Selecione...</option>'
    + CATEGORIAS.map((c) => `<option value="${c}">${c}</option>`).join("");

  filtroCategoria.innerHTML = '<option value="">Todas as categorias</option>'
    + CATEGORIAS.map((c) => `<option value="${c}">${c}</option>`).join("");
}

function escaparHtml(texto) {
  const div = document.createElement("div");
  div.textContent = texto ?? "";
  return div.innerHTML;
}

function renderLista() {
  const lista = listarItensFiltrados();
  const container = document.getElementById("listaItens");
  const mensagemVazio = document.getElementById("mensagemLista");

  if (lista.length === 0) {
    mensagemVazio.hidden = false;
    mensagemVazio.textContent = itens.length === 0
      ? "Nenhum item cadastrado ainda."
      : "Nenhum item encontrado com esses filtros.";
    container.innerHTML = "";
    return;
  }
  mensagemVazio.hidden = true;

  container.innerHTML = lista.map((item) => cardItemHtml(item)).join("");

  container.querySelectorAll("[data-acao='editar']").forEach((btn) => {
    btn.addEventListener("click", () => {
      const item = itens.find((i) => i.id === btn.dataset.id);
      if (item) entrarModoEdicao(item);
    });
  });
  container.querySelectorAll("[data-acao='resolver']").forEach((btn) => {
    btn.addEventListener("click", () => {
      const item = itens.find((i) => i.id === btn.dataset.id);
      if (item) { item.resolvido = true; renderLista(); }
    });
  });
  container.querySelectorAll("[data-acao='remover']").forEach((btn) => {
    btn.addEventListener("click", () => {
      const item = itens.find((i) => i.id === btn.dataset.id);
      if (item && confirm(`Remover o item "${item.descricao}"?`)) {
        if (idEmEdicao === item.id) sairModoEdicao();
        itens = itens.filter((i) => i.id !== btn.dataset.id);
        renderLista();
      }
    });
  });
}

function cardItemHtml(item) {
  const dataFormatada = new Date(item.criadoEm).toLocaleDateString("pt-BR");
  const chips = (item.caracteristicas || [])
    .map((c) => `<span class="chip">${escaparHtml(c)}</span>`).join("");
  const emEdicao = item.id === idEmEdicao;

  return `
    <div class="card item ${item.resolvido ? "resolvido" : ""} ${emEdicao ? "em-edicao" : ""}">
      <div class="item-topo">
        <span class="tag ${item.tipo === "PERDIDO" ? "perdido" : "encontrado"}">
          ${item.tipo === "PERDIDO" ? "Perdido" : "Encontrado"}
        </span>
        <span class="item-categoria">${escaparHtml(item.categoria)}</span>
        ${item.resolvido ? '<span class="tag resolvido-tag">Resolvido</span>' : ""}
        ${emEdicao ? '<span class="tag resolvido-tag">Editando</span>' : ""}
      </div>
      <p class="item-descricao">${escaparHtml(item.descricao)}</p>
      <div class="item-meta">
        <span>📍 ${escaparHtml(item.localizacao)}</span>
        <span>🗓️ ${dataFormatada}</span>
      </div>
      ${chips ? `<div class="item-caracteristicas">${chips}</div>` : ""}
      <div class="item-contato">Contato: <b>${escaparHtml(item.contatoNome)}</b> — ${escaparHtml(item.contatoTelefone)}</div>
      <div class="item-acoes">
        <button type="button" class="botao-texto editar" data-acao="editar" data-id="${item.id}">Editar</button>
        ${!item.resolvido ? `<button type="button" class="botao-texto" data-acao="resolver" data-id="${item.id}">Marcar como resolvido</button>` : ""}
        <button type="button" class="botao-texto perigo" data-acao="remover" data-id="${item.id}">Remover</button>
      </div>
    </div>`;
}

/* ============================================================
   Modo de edição do formulário
   ============================================================ */

function selecionarTipoNoFormulario(tipo) {
  tipoSelecionado = tipo;
  document.getElementById("campoTipo").value = tipo;
  document.querySelectorAll("#segmentadoTipo button").forEach((b) => {
    b.classList.toggle("ativo", b.dataset.tipo === tipo);
  });
}

function entrarModoEdicao(item) {
  idEmEdicao = item.id;

  document.getElementById("campoIdEdicao").value = item.id;
  selecionarTipoNoFormulario(item.tipo);
  document.getElementById("campoCategoria").value = item.categoria;
  document.getElementById("campoDescricao").value = item.descricao;
  document.getElementById("campoLocalizacao").value = item.localizacao;
  document.getElementById("campoCaracteristicas").value = (item.caracteristicas || []).join(", ");
  document.getElementById("campoNome").value = item.contatoNome;
  document.getElementById("campoTelefone").value = item.contatoTelefone;

  document.getElementById("tituloFormulario").textContent = "Editar objeto";
  const botaoSubmit = document.getElementById("botaoSubmit");
  botaoSubmit.textContent = "Salvar edição";
  botaoSubmit.classList.add("editando");
  document.getElementById("botaoCancelarEdicao").hidden = false;
  document.getElementById("formCadastro").classList.add("editando");

  document.getElementById("mensagemForm").hidden = true;
  document.getElementById("formCadastro").scrollIntoView({ behavior: "smooth", block: "start" });
  renderLista();
}

function sairModoEdicao() {
  idEmEdicao = null;
  document.getElementById("campoIdEdicao").value = "";

  document.getElementById("formCadastro").reset();
  selecionarTipoNoFormulario("PERDIDO");
  document.getElementById("campoCategoria").selectedIndex = 0;

  document.getElementById("tituloFormulario").textContent = "Cadastrar objeto";
  const botaoSubmit = document.getElementById("botaoSubmit");
  botaoSubmit.textContent = "Cadastrar";
  botaoSubmit.classList.remove("editando");
  document.getElementById("botaoCancelarEdicao").hidden = true;
  document.getElementById("formCadastro").classList.remove("editando");
}

/* ============================================================
   Salvar / carregar dados em arquivo (sem depender de servidor)
   ============================================================ */

function salvarEmArquivo() {
  const blob = new Blob([JSON.stringify(itens, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  const dataHoje = new Date().toISOString().slice(0, 10);
  a.href = url;
  a.download = `objetos-perdidos-${dataHoje}.json`;
  a.click();
  URL.revokeObjectURL(url);
}

function carregarDeArquivo(arquivo) {
  const leitor = new FileReader();
  leitor.onload = () => {
    try {
      const dados = JSON.parse(leitor.result);
      if (!Array.isArray(dados)) throw new Error("Formato inválido");
      itens = dados;
      sairModoEdicao();
      renderLista();
      alert(`${dados.length} item(ns) carregado(s) com sucesso.`);
    } catch (e) {
      alert("Não foi possível ler esse arquivo. Confira se é um arquivo salvo por este mesmo sistema.");
    }
  };
  leitor.readAsText(arquivo, "utf-8");
}

/* ============================================================
   Eventos da interface
   ============================================================ */

function mostrarMensagemForm(texto, tipo) {
  const el = document.getElementById("mensagemForm");
  el.textContent = texto;
  el.className = `mensagem ${tipo}`;
  el.hidden = false;
}

document.addEventListener("DOMContentLoaded", () => {
  preencherSelectsDeCategoria();
  renderLista();

  // Seletor de tipo (perdido/encontrado) no formulário
  document.getElementById("segmentadoTipo").addEventListener("click", (e) => {
    const btn = e.target.closest("button[data-tipo]");
    if (!btn) return;
    selecionarTipoNoFormulario(btn.dataset.tipo);
  });

  // Filtro por tipo
  document.getElementById("segmentadoFiltroTipo").addEventListener("click", (e) => {
    const btn = e.target.closest("button[data-tipo]");
    if (!btn) return;
    filtros.tipo = btn.dataset.tipo;
    document.querySelectorAll("#segmentadoFiltroTipo button").forEach((b) => {
      b.classList.toggle("ativo", b === btn);
    });
    renderLista();
  });

  document.getElementById("filtroCategoria").addEventListener("change", (e) => {
    filtros.categoria = e.target.value;
    renderLista();
  });

  document.getElementById("filtroLocalizacao").addEventListener("input", (e) => {
    filtros.localizacao = e.target.value;
    renderLista();
  });

  // Envio do formulário de cadastro / edição
  document.getElementById("formCadastro").addEventListener("submit", (e) => {
    e.preventDefault();

    const caracteristicas = document.getElementById("campoCaracteristicas").value
      .split(",").map((s) => s.trim()).filter(Boolean);

    const dados = {
      tipo: tipoSelecionado,
      categoria: document.getElementById("campoCategoria").value,
      descricao: document.getElementById("campoDescricao").value,
      localizacao: document.getElementById("campoLocalizacao").value,
      caracteristicas,
      contatoNome: document.getElementById("campoNome").value,
      contatoTelefone: document.getElementById("campoTelefone").value,
    };

    if (!dados.categoria || !dados.descricao.trim() || !dados.localizacao.trim()
      || !dados.contatoNome.trim() || !dados.contatoTelefone.trim()) {
      mostrarMensagemForm("Preencha todos os campos obrigatórios.", "erro");
      return;
    }

    if (idEmEdicao) {
      const idAtualizado = idEmEdicao;
      atualizarItem(idAtualizado, dados);
      sairModoEdicao();
      mostrarMensagemForm("Item atualizado com sucesso!", "sucesso");
    } else {
      const item = cadastrarItem(dados);
      mostrarMensagemForm(`Item cadastrado com sucesso! ID: ${item.id}`, "sucesso");
      e.target.reset();
      selecionarTipoNoFormulario("PERDIDO");
      document.getElementById("campoCategoria").selectedIndex = 0;
    }

    renderLista();
  });

  // Cancelar edição
  document.getElementById("botaoCancelarEdicao").addEventListener("click", () => {
    sairModoEdicao();
    renderLista();
  });

  // Salvar / carregar arquivo
  document.getElementById("btnSalvarArquivo").addEventListener("click", salvarEmArquivo);
  document.getElementById("inputCarregarArquivo").addEventListener("change", (e) => {
    if (e.target.files[0]) carregarDeArquivo(e.target.files[0]);
    e.target.value = "";
  });
});
