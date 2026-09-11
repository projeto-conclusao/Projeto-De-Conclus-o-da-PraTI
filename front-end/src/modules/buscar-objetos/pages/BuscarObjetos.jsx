import { useState } from 'react'; 
import './buscarObjetos.css';

import  FilterProvider  from '../../../contexts/buscarObjetosContexts/filterContext.jsx';

import sumaryIcon from '../../../assets/icons/buscarObjetos/sumary.svg';
import arrowGray from '../../../assets/icons/buscarObjetos/arrow-gray.png'

import PrimaryFilters from '../components/primaryFilters/PrimaryFilters.jsx';
import SecondaryFilters from '../components/secondaryFilters/SecondaryFilters.jsx'; 
import PrincipalCards from '../components/principalCards/PrincipalCards.jsx';


function BuscarObjetos(){

    const [pagina, setPagina] = useState(1); 

    const total = 80
    const totalPorPagina = 6;
    const totalDePaginas = [];
    const totalDePaginasNumerico = Math.ceil(total / totalPorPagina)

    for(let i = 1; i < totalDePaginasNumerico + 1; i++){
        totalDePaginas.push(i)
    }

    let inicio = totalPorPagina * (pagina - 1);
    let fim = inicio + totalPorPagina;

    function alternarPagina(pagina){
        setPagina(pagina);
    }

    return (
        <FilterProvider >
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
                    {<PrimaryFilters key='' />}
                </div>

                <div className='container__card'>
                    <SecondaryFilters key='' />
                </div>

                <div className='principal container__card'>
                    <div className='container__principal'>

                        <div className='principal__cabecalho'>
                            <span className='text__principal'> {} objetos encontrados </span>
                            <p className="subtitle">Resultados relacionados à sua busca</p>
                        </div>

                        <div className='principal__cards'>
                            <PrincipalCards  key='' inicio={inicio} fim={fim}/>
                        </div>

                        <div className='principal__footer'>
                            <span>mostrando {fim > total ? total : fim} de {total} resultados</span>

                            <div className="paginas__principal">

                                <div onClick={() => pagina < 1 ?  setPagina(1) : setPagina(pagina - 1)} className='button button__arrow'>
                                        <img className='arrow__left' src={arrowGray} />
                                </div>

                                {totalDePaginas.map((valor) => {
                                    return (<p onClick={() => {alternarPagina(valor)}} className={pagina === valor ? 'paginas pagina__ativa' : 'paginas'}> {valor} </p>)
                                })}

                                <div onClick={() => pagina > totalDePaginasNumerico ? setPagina(totalDePaginasNumerico) : setPagina(pagina + 1)} className='button button__arrow'>
                                        <img className='arrow__right' src={arrowGray} />
                                </div>

                            </div>
                        </div>

                    </div>

                </div>



            </section>
        </FilterProvider>
    )
}

export default BuscarObjetos;