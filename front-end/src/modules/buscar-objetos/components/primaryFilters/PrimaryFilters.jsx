import { useState } from 'react';
import { useFilter } from '../../../../hooks/buscarObjetos/filterHook.jsx';
import './primaryFilters.css';

import Dropdown from '../dropdown/Dropdown.jsx'; 
import DateRange from '../dateRange/DateRange.jsx';

import lupaIcon from '../../../../assets/icons/buscarObjetos/lupa.svg';
import lupaGray from '../../../../assets/icons/buscarObjetos/lupa-gray.svg';
import pinGray from '../../../../assets/icons/buscarObjetos/pin-gray.svg';
import gridGray from '../../../../assets/icons/buscarObjetos/grid-gray.svg';
import calendarGray from '../../../../assets/icons/buscarObjetos/calendar-gray.svg';
import arrowGray from '../../../../assets/icons/buscarObjetos/arrow-gray.png';
import closeGray from '../../../../assets/icons/buscarObjetos/x-gray.png';

import filtros from './primaryFilters.json'; 

const icons = {
    lupaGray,
    pinGray,
    gridGray,
    calendarGray,
}

function BasicFilters(){

    const {dispatch} = useFilter();
    const [valor, setValor] = useState({}); 
    

    function pegarValorInput(categoria, valor){
       setValor((valorAnterior) => ({
        ...valorAnterior,
        [categoria.toUpperCase()]: valor.toUpperCase()
       })); 
    }

    function filtrar(){
        dispatch({ type: 'change', value: valor});
    }

    // Aqui vai ficar a requisicao http solicitando todas as categorias registradas
    const categoriasTeste = [
        {
            id: 1, 
            nome: 'eletronicos',
        },
        {
            id: 2, 
            nome: 'bolas',
        },
        {
            id: 3, 
            nome: 'tacos',
        },
    ]

    return (
        
        <div className='filter'>

            {filtros.map((valor) => { 
                return <div>
                            <label htmlFor={valor.categoria} className="text__filter" > {valor.categoria} </label>
                                    
                            <div className='input__container'>
                                <div>

                                    {valor.categoria === 'Categoria' 
                                    ? <Dropdown id={valor.categoria} arrow={arrowGray} close={closeGray} options={categoriasTeste} type={valor.type} placeholder= {valor.placeholder} change={pegarValorInput} /> 
                                    : valor.categoria === 'Período' 
                                        ? <DateRange placeholder= {valor.placeholder} arrow={arrowGray} close={closeGray} change={pegarValorInput} />
                                        : <input id= {valor.categoria} onChange={(e) => {pegarValorInput(valor.categoria, e.target.value)}} 
                                        className="input__filter" type={valor.type} placeholder= {valor.placeholder} />}

                                    <img className="icon__filter" src= {icons[valor.icon]} /> 

                                </div>
                            </div>
                        </div>       
            })}

                    <button className='button buscar' onClick={() => {filtrar()}}>
                            <img className="lupa__icon" src={lupaIcon} alt="" />
                            <span className='buscar__text'>buscar</span>
                    </button>

        </div>
    )
}

export default BasicFilters;