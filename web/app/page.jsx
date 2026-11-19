'use client';

import { useEffect, useRef, useState } from 'react';
import {
  Hamburger, ShoppingBag, Plus, Minus, CreditCard, QrCode,
  CircleCheck, CircleAlert, ReceiptText, RotateCw,
} from 'lucide-react';

const produtosVisuais = {
  HAMBURGUER: { categoria: 'Sanduíche', foto: 'hamburguer' },
  BATATA: { categoria: 'Acompanhamento', foto: 'batata' },
  REFRIGERANTE: { categoria: 'Bebida', foto: 'refrigerante' },
  SUCO: { categoria: 'Bebida', foto: 'suco' },
};

const pagamentos = [
  { id: 'PIX', nome: 'Pix', Icone: QrCode },
  { id: 'CARTAO', nome: 'Cartão', Icone: CreditCard },
];

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const reais = (centavos) => moeda.format(centavos / 100);

export default function Cardapio() {
  const [produtos, setProdutos] = useState([]);
  const [catalogo, setCatalogo] = useState('carregando');
  const [carrinho, setCarrinho] = useState({});
  const [pagamento, setPagamento] = useState('PIX');
  const [enviando, setEnviando] = useState(false);
  const [retorno, setRetorno] = useState(null);
  const [tentativaPendente, setTentativaPendente] = useState(false);
  const envioEmCurso = useRef(false);
  const ultimaTentativa = useRef(null);

  async function consultarCardapio(sinal) {
    setCatalogo('carregando');
    try {
      const resposta = await fetch('/api/cardapio', { cache: 'no-store', signal: sinal });
      if (!resposta.ok) throw new Error('Cardápio indisponível');
      setProdutos(await resposta.json());
      setCatalogo('pronto');
    } catch {
      if (!sinal?.aborted) setCatalogo('erro');
    }
  }

  useEffect(() => {
    const consulta = new AbortController();
    consultarCardapio(consulta.signal);
    return () => consulta.abort();
  }, []);

  const itens = produtos.filter((produto) => carrinho[produto.id] > 0);
  const quantidadeTotal = itens.reduce((soma, produto) => soma + carrinho[produto.id], 0);
  const total = itens.reduce((soma, produto) => soma + produto.precoCentavos * carrinho[produto.id], 0);
  const bloqueado = enviando || tentativaPendente;

  function alterarQuantidade(produto, variacao) {
    setRetorno(null);
    setCarrinho((atual) => {
      const quantidade = atual[produto.id] ?? 0;
      if (variacao > 0 && quantidade >= produto.estoque) return atual;
      const atualizado = { ...atual };
      if (quantidade + variacao <= 0) delete atualizado[produto.id];
      else atualizado[produto.id] = quantidade + variacao;
      return atualizado;
    });
  }

  async function finalizarPedido(evento) {
    evento.preventDefault();
    if (envioEmCurso.current || itens.length === 0) return;
    envioEmCurso.current = true;
    setEnviando(true);
    setRetorno(null);

    try {
      const dados = new URLSearchParams({
        tentativa: ultimaTentativa.current ??= crypto.randomUUID(),
        pagamento,
      });
      for (const produto of itens) dados.set(produto.id, carrinho[produto.id]);
      const resposta = await fetch('/api/pedidos', { method: 'POST', body: dados });
      if (resposta.status >= 500) throw new Error('Pedido sem confirmação');
      const resultado = await resposta.json();
      if (!resposta.ok) {
        setRetorno({ aprovado: false, mensagem: resultado.mensagem });
      } else {
        setProdutos(resultado.produtos);
        setRetorno(resultado);
        if (resultado.aprovado) setCarrinho({});
      }
      ultimaTentativa.current = null;
      setTentativaPendente(false);
    } catch {
      setTentativaPendente(true);
      setRetorno({ aprovado: false, mensagem: 'Não foi possível confirmar o pedido. Tente novamente para consultar o resultado da mesma tentativa.' });
    } finally {
      envioEmCurso.current = false;
      setEnviando(false);
    }
  }

  return (
    <>
      <header className="cabecalho">
        <div className="largura barra">
          <a className="marca" href="#inicio">
            <span className="marca-icone"><Hamburger size={23} aria-hidden="true" /></span>
            Ponto do Lanche
          </a>
          <nav aria-label="Navegação principal">
            <a className="nav-cardapio" href="#cardapio">Cardápio</a>
            <a className="atalho-pedido" href="#pedido">
              <ShoppingBag size={17} aria-hidden="true" /> Meu pedido <span>{quantidadeTotal}</span>
            </a>
          </nav>
        </div>
      </header>

      <main id="inicio" className="largura">
        <section className="hero" aria-labelledby="titulo">
          <div className="hero-foto" />
          <div className="hero-texto">
            <h1 id="titulo">MONTE SEU<br />PEDIDO<span>.</span></h1>
            <p>Escolha seus itens e finalize com Pix ou cartão.</p>
            <a className="botao-cardapio" href="#cardapio">Ver cardápio</a>
          </div>
        </section>

        <div className="balcao">
          <section id="cardapio" aria-labelledby="titulo-cardapio">
            <div className="titulo-secao">
              <h2 id="titulo-cardapio">Cardápio</h2>
              {catalogo === 'pronto' && <span>{produtos.length} produtos</span>}
            </div>
            {catalogo === 'carregando' && <p className="aviso-catalogo" role="status">Carregando o cardápio…</p>}
            {catalogo === 'erro' && (
              <div className="aviso-catalogo" role="status">
                <p>O cardápio está indisponível. Confira se a API Java está em execução.</p>
                <button type="button" className="botao-cardapio" onClick={() => consultarCardapio()}>
                  <RotateCw size={16} aria-hidden="true" /> Tentar novamente
                </button>
              </div>
            )}
            {catalogo === 'pronto' && (
              <div className="grade">
                {produtos.map((produto) => (
                  <ProdutoCard key={produto.id} produto={produto}
                    quantidade={carrinho[produto.id] ?? 0} bloqueado={bloqueado}
                    adicionar={() => alterarQuantidade(produto, 1)} />
                ))}
              </div>
            )}
          </section>

          <aside id="pedido" className="pedido" aria-labelledby="titulo-pedido">
            <div className="pedido-cabecalho">
              <div className="sacola"><ShoppingBag size={21} aria-hidden="true" /></div>
              <div><h2 id="titulo-pedido">Meu pedido</h2><p>{quantidadeTotal} {quantidadeTotal === 1 ? 'item' : 'itens'}</p></div>
            </div>
            {itens.length === 0 && !retorno && (
              <div className="vazio">
                <ReceiptText size={31} strokeWidth={1.4} aria-hidden="true" />
                <strong>Seu carrinho está vazio</strong>
                <p>Adicione um item do cardápio para começar.</p>
              </div>
            )}
            <form onSubmit={finalizarPedido}>
              {itens.length > 0 && (
                <ul className="carrinho">
                  {itens.map((produto) => (
                    <li key={produto.id}>
                      <div><strong>{produto.nome}</strong><span>{reais(produto.precoCentavos * carrinho[produto.id])}</span></div>
                      <div className="quantidade">
                        <button type="button" aria-label={`Diminuir ${produto.nome}`} disabled={bloqueado}
                          onClick={() => alterarQuantidade(produto, -1)}><Minus size={15} aria-hidden="true" /></button>
                        <span aria-label="Quantidade">{carrinho[produto.id]}</span>
                        <button type="button" aria-label={`Aumentar ${produto.nome}`}
                          disabled={bloqueado || carrinho[produto.id] >= produto.estoque}
                          onClick={() => alterarQuantidade(produto, 1)}><Plus size={15} aria-hidden="true" /></button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
              <div aria-live="polite">
                {retorno && (
                  <div className={`resultado ${retorno.aprovado ? 'sucesso' : 'falha'}`}>
                    {retorno.aprovado ? <CircleCheck size={19} aria-hidden="true" /> : <CircleAlert size={19} aria-hidden="true" />}
                    <p>{retorno.mensagem}</p>
                  </div>
                )}
              </div>
              <div className="fechamento">
                <div className="total"><span>Total do pedido</span><strong>{reais(total)}</strong></div>
                <fieldset disabled={bloqueado || itens.length === 0}>
                  <legend>Pagamento simulado</legend>
                  <div className="pagamentos">
                    {pagamentos.map(({ id, nome, Icone }) => (
                      <label key={id}>
                        <input type="radio" name="pagamento" value={id} checked={pagamento === id}
                          onChange={() => { setPagamento(id); setRetorno(null); }} />
                        <span><Icone size={19} aria-hidden="true" /> {nome}</span>
                      </label>
                    ))}
                  </div>
                </fieldset>
                <button className="finalizar" type="submit" disabled={enviando || itens.length === 0}>
                  {enviando ? 'Confirmando…' : tentativaPendente ? 'Tentar novamente' : 'Finalizar pedido'}
                </button>
              </div>
            </form>
          </aside>
        </div>
      </main>
      <footer className="largura rodape">Ponto do Lanche · Projeto APS 2</footer>
    </>
  );
}

function ProdutoCard({ produto, quantidade, bloqueado, adicionar }) {
  const visual = produtosVisuais[produto.id];
  const esgotado = produto.estoque === 0;
  return (
    <article className="produto">
      <div className={`produto-foto ${visual?.foto ?? ''}`}>
        {visual && <img src="/produtos.webp" alt="" />}
        <span className={esgotado ? 'estoque esgotado' : 'estoque'}>
          {esgotado ? 'Esgotado' : `${produto.estoque} em estoque`}
        </span>
      </div>
      <div className="produto-texto">
        {visual && <p>{visual.categoria}</p>}
        <h3>{produto.nome}</h3>
        <div className="produto-acao">
          <strong>{reais(produto.precoCentavos)}</strong>
          <button type="button" aria-label={`Adicionar ${produto.nome}`}
            disabled={bloqueado || quantidade >= produto.estoque} onClick={adicionar}>
            <Plus size={17} aria-hidden="true" /> Adicionar
          </button>
        </div>
      </div>
    </article>
  );
}
