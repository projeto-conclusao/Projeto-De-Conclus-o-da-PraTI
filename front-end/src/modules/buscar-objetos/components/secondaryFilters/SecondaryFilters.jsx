import { useState } from 'react';
import { useOutletContext } from 'react-router'; 
import { useFilter }  from '../../../../hooks/buscarObjetos/filterHook.jsx'; 
import './secondaryFilters.css'; 

import todosWhite from '../../../../assets/icons/buscarObjetos/grid-white.png';
import perdidosWhite from '../../../../assets/icons/buscarObjetos/warning-white.png';
import encontradosWhite from '../../../../assets/icons/buscarObjetos/lupa-white.svg';
import clockWhite from '../../../../assets/icons/buscarObjetos/clock-white.svg';
import starWhite from '../../../../assets/icons/buscarObjetos/star-white.png';

import todosBlack from '../../../../assets/icons/buscarObjetos/grid-black.png';
import perdidosBlack from '../../../../assets/icons/buscarObjetos/warning-black.png';
import encontradosBlack from '../../../../assets/icons/buscarObjetos/lupa-black.svg';
import clockBlack from '../../../../assets/icons/buscarObjetos/clock-black.svg';
import starBlack from '../../../../assets/icons/buscarObjetos/star-black.png';

import filtros from './secondaryFilters.json';

const icons = {
    todosWhite,
    perdidosWhite,
    encontradosWhite,
    clockWhite,
    starWhite,
    todosBlack,
    perdidosBlack,
    encontradosBlack,
    clockBlack,
    starBlack
}

function SecondaryFilters(){

    const [ativo, setAtivo] = useState('Todos');
    const [tema] = useOutletContext();

    const {dispatch} = useFilter();


    function alternarAtivo(value){
        dispatch({ type: 'add', value: value.toUpperCase() })
        setAtivo(value);
    }

    function AlternarCategoria({categoria, iconWhite, iconBlack}){

        if(tema === 'light'){
            return ativo === categoria ? (<img className="icon__secondary__filter" src= {icons[iconWhite]} />) 
            : (<img className="icon__secondary__filter" src= {icons[iconBlack]} />);
        }
        
        if(tema === 'dark'){
            return (<img className="icon__secondary__filter" src= {icons[iconWhite]} />)
        }
    }

    return (
        <div className='filter'>

            {filtros.map((valor) => { 
                return <div>
                            <div className='input__container'>

                                <div className={ativo === valor.categoria ? 'button__secondary__filter secondary__active' : 'button__secondary__filter'}
                                onClick={(() => {alternarAtivo(valor.categoria)})} >
                                    
                                    <span className='text__secondary__filters'> {valor.categoria} </span>
                                    {<AlternarCategoria categoria={valor.categoria} iconWhite={valor.iconWhite} iconBlack={valor.iconBlack} />}

                                </div>
                            </div>
                        </div>
            })}
        </div>
    )
}

export default SecondaryFilters;