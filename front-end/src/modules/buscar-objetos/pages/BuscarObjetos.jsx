import './buscarObjetos.css';

import sumaryIcon from '../../../assets/icons/buscarObjetos/sumary.svg'
import lupaIcon from '../../../assets/icons/buscarObjetos/lupa.svg'

import PrimaryFilters from '../components/primaryFilters/PrimaryFilters.jsx';
import SecondaryFilters from '../components/secondaryFilters/SecondaryFilters.jsx'; 
import PrincipalCards from '../components/principalCards/PrincipalCards.jsx';

function BuscarObjetos(){




    return (
        <section className="buscar__objetos">

            <div className="head">

                <div className="left">
                    <h1 className="title__buscar">Buscar objetos</h1>
                    <p className="subtitle">Encontre objetos perdidos ou verifique se alguém encontrou oque você procura.</p>
                </div>

                <div className="right">
                    <div>
                        <h2 className="head__encontrados">1.245</h2>
                        <p className="subtitle">objetos encontrados</p>
                    </div>

                    <img className="sumary__icon" src={sumaryIcon} alt="" />
                </div>

            </div>

            <div className='container__card'>
                <PrimaryFilters />

                <button className='button buscar'>
                        <img className="lupa__icon" src={lupaIcon} alt="" />
                        <span className='buscar__text'>buscar</span>
                </button>
            </div>

            <div className='container__card'>
                <SecondaryFilters />
            </div>

            <div className='principal container__card'>
                <div className='container__principal'>

                    <div className='principal__cabecalho'>
                        <span className='text__principal'> {} objetos encontrados </span>
                        <p className="subtitle">Resultados relacionados à sua busca</p>
                    </div>

                    <div className='principal__cards'>
                        <PrincipalCards />
                    </div>

                    <div>
                        <span>mostrando {} de {} resultados</span>
                    </div>

                </div>

            </div>



        </section>
    )
}

export default BuscarObjetos;