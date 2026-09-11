import "./hero.css";
import { useOutletContext } from "react-router";

import direitawhite from "../../../assets/icons/home/direita-white.png";
import direitablack from "../../../assets/icons/home/direita-black.png";
import objeto from "../../../assets/icons/home/objeto.png";
import localizacao from "../../../assets/icons/home/localizacao.png";
import locIcon from "../../../assets/icons/home/loc-icon.png";
import dataIcon from "../../../assets/icons/home/data-icon.png";
import categoria from "../../../assets/icons/home/categoria.png";
import CadastroaDevolucaoWhite from "../../../assets/icons/home/CadastroaDevolucao-white.png";
import CadastroaDevolucaoBlack from "../../../assets/icons/home/CadastroaDevolucao-black.png";
import EstatisticasWhite from "../../../assets/icons/home/Estatisticas-white.png";
import EstatisticasBlack from "../../../assets/icons/home/Estatisticas-black.png";

function Hero() {
    const { tema } = useOutletContext();

    return (

        <section className="hero">

            {/* Lado Esquerdo */}

            <div className="hero-content">

                <h1>
                    Seu objeto pode estar
                    <br />
                    mais <span id="perto">perto</span> do que você
                    <br />
                    <span id="imagina">imagina.</span>
                </h1>

                <p>
                    Cadastre o que você perdeu ou encontrou.
                    <br />
                    Nós ajudamos você a encontrar o que procura.
                </p>

                <div className="hero-buttons">

                    <button className="btn_perdiObjeto">
                        Perdi um objeto
                    </button>

                    <button className="btn_encontreiObjeto">
                        Encontrei um objeto
                    </button>
                </div>

            </div>


            {/* Lado Direito */}

          <div className="hero-imagem">
            {tema === 'light' ? <img src={direitawhite} alt="Imagem ilustrativa" /> : <img src={direitablack} alt="Imagem ilustrativa" />}
          </div>

            {/* Campo de Busca*/}

        <div className="hero-busca"> 
            
            <h2>O que você está procurando?</h2>

            <div className="objeto">
                {tema === 'light' ? <img src={objeto} alt="Imagem ilustrativa" /> : <img src={objeto} alt="Imagem ilustrativa" />}
                <label htmlFor="objeto">Objeto</label>
                <input type="text" placeholder="EX.: Carteira, Celular, Chave ..." />
            </div>

            <div className="localizacao">
                {tema === 'light' ? <img src={localizacao} alt="Imagem ilustrativa" /> : <img src={localizacao} alt="Imagem ilustrativa" />}
                <label htmlFor="localizacao">Localização</label>
                <input type="text" placeholder="EX.: Porto Alegre, Rio Grande ..." />
            </div>

            <div className="categoria">
                {tema === 'light' ? <img src={categoria} alt="Imagem ilustrativa" /> : <img src={categoria} alt="Imagem ilustrativa" />}
                <label htmlFor="categoria">Categoria</label>

                <select>
                    <option>Todas as Categorias</option>
                    <option>Documentos</option>
                    <option>Eletrônicos</option>
                    <option>Roupas</option>
                    <option>Chaves</option>
                    <option>Outros</option>
                </select>
            </div>
            
            <div>
                <button className="btn_buscar">
                    Buscar
                </button>
            </div>

    </div>

            {/* Seção de Cadastro e Estatísticas */}

        <div className="cadastro-estatisticas">

            <div className="cadastro-imagem">
                {tema === 'light' ? <img src={CadastroaDevolucaoWhite} alt="Imagem ilustrativa" /> : <img src={CadastroaDevolucaoBlack} alt="Imagem ilustrativa" />}
            </div>

            <div className="estatisticas">
               {tema === 'light' ? <img src={EstatisticasWhite} alt="Imagem ilustrativa" /> : <img src={EstatisticasBlack} alt="Imagem ilustrativa" />}
            </div>
        </div>

            {/*Seção obejtos recentes*/}

        <div className="objetos-recentes">
            
            <h2>Objetos recentemente cadastrados</h2>
            <a href="#">
            Ver todos →
            </a>

        </div>

        <div className="objetos-lista">
            {objetos.map((objeto) => {
                const correspondencia = getCorrespondenciaConfig(objeto);

                return (
                <div className="objeto-card" key={objeto.id}>
                    <div className="objeto-topo">
                        <div className="objeto-imagem">
                            <div className="imagem-placeholder">
                                FOTO
                            </div>
                        </div>

                        <div className="objeto-titulo-wrap">
                            <h3>{objeto.nome}</h3>
                            <span
                                className={`status ${
                                    objeto.status === "Perdido" ? "perdido" : "encontrado"
                                }`}
                            >
                                {objeto.status}
                            </span>
                        </div>
                    </div>

                    <div className="objetos-info">
                        <p>
                            <img className="info-icone" src={locIcon} alt="Localização" />
                            {objeto.localizacao}
                        </p>
                        <p>
                            <img className="info-icone" src={dataIcon} alt="Data" />
                            {objeto.data}
                        </p>
                    </div>

                    <button className={`btn_correspondencia ${correspondencia.classe}`}>
                        {correspondencia.texto}
                    </button>
                </div>
                );
            })}
        </div>

        </section>
    );

    
}

const objetos = [
    {
        id: 1,
        nome: "Carteira preta",
        status: "Encontrado",
        localizacao: "Porto Alegre",
        data: "11/09/2026, 14:30",
        match: false
    },
    {
        id: 2,
        nome: "Celular Samsung",
        status: "Perdido",
        localizacao: "Santo Antônio",
        data: "10/09/2026, 09:15",
        match: true
    },
    {
        id: 3,
        nome: "Chaveiro azul",
        status: "Encontrado",
        localizacao: "Centro",
        data: "11/09/2026, 08:45",
        match: false
    },
    {
        id: 4,
        nome: "Bolsa de viagem",
        status: "Perdido",
        localizacao: "Cidade Baixa",
        data: "09/09/2026, 18:10",
        match: false
    }
];

const getCorrespondenciaConfig = (objeto) => {
    if (objeto.status === "Encontrado") {
        return {
            texto: "Encontrado",
            classe: "correspondencia-encontrado"
        };
    }

    if (objeto.match) {
        return {
            texto: "Possível correspondência",
            classe: "correspondencia-match"
        };
    }

    return {
        texto: "Sem correspondência",
        classe: "correspondencia-sem"
    };
};

export default Hero;