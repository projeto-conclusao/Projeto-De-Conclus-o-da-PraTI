
import "./Hero.css";
import { useOutletContext } from "react-router";

import direitawhite from "../../../assets/icons/home/direita-white.png";
import direitablack from "../../../assets/icons/home/direita-black.png";

function Hero() {
    const { tema } = useOutletContext();
    console.log(tema);
    return (

        <section className="hero">

            {/* Lado Esquerdo */}

            <div className="hero-content">

                <h1>
                    Seu objeto pode estar
                    <br />
                    mais <span>perto</span> do que você
                    <br />
                    imagina.
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

        </section>
    );
}

export default Hero;