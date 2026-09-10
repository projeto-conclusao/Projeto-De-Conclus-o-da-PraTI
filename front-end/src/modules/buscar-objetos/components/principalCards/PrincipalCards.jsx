import { useState } from 'react';
import { NavLink } from 'react-router'; 

import './principalCards.css'; 

import pinGray from '../../../../assets/icons/buscarObjetos/pin-gray.svg';
import calendarGray from '../../../../assets/icons/buscarObjetos/calendar-gray.svg';
import warningRed from '../../../../assets/icons/buscarObjetos/warning-red.svg';
import checkGreen from '../../../../assets/icons/buscarObjetos/check-green.svg';
import hourglassOrange from '../../../../assets/icons/buscarObjetos/hourglass-orange.svg';
import arrowBlue from '../../../../assets/icons/buscarObjetos/arrow-blue.svg';

// Vai receber de buscarObjetos.jsx depois
const objetos = [
    {
        id: 1,
        imagem: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRrY_4KkPEHad6YFRowx4F7glLuwXbCigLZPQVC0I1P1pF5EFCB5zYq-8c&s=10',
        nome: 'Bola de Basquete Poker', 
        status: 'PERDIDO',
        categoria: 'Bolas', 
        iconCategoria: 'https://img.icons8.com/?size=100&id=kK2OkfQGPS4B&format=png&color=737373',
        descricaoBreve: 'Iphone 13 azul com pequenos sinais de uso, encontrada próxima ao Posto 8 da praia de Ipanema verde',
        endereco: 'Ipanema',
        cidade: 'Rio de Janeiro',
        dataOcorrencia: 'Hoje, 12:30'
    }
]

function PrincipalCards(){


    function VerificaIconStatus(){
        return objetos.map((valor) => {
            if (valor.status === 'PERDIDO'){ return ( <img className="image__principal" src={warningRed} alt="" />)  }
            if (valor.status === 'ENCONTRADO') { return ( < img className="image__principal" src={checkGreen} alt="" />)}
            if (valor.status === 'ANALISE') { return (<img className="image__principal" src={hourglassOrange} alt="" />)}})
    }

    function adicionaClassNameStatus(){
        return objetos.map((valor) => {
            if (valor.status === 'PERDIDO'){ return 'status__red'  }
            if (valor.status === 'ENCONTRADO') { return 'status__green'}
            if (valor.status === 'ANALISE') { return 'status__orange'}})
    }

    return (
        <div>
            {objetos.map((valor) => {
                return (<div className='card__principal'>
                            <img className='image__card ' src={valor.imagem} alt="" />

                            <div className='principal__meio'>

                                <div className='principal__encima'>

                                    <h1 className='titulo__principal'>{valor.nome}</h1>

                                    <div className='categoria__status'>
                                        <div className='status__principal'>
                                            <div className={adicionaClassNameStatus()}>

                                                <VerificaIconStatus/>
                                                <span>{valor.status}</span>

                                            </div>
                                        </div>

                                        <div className='categoria__principal'>
                                            <img className='icon__categoria'src={valor.iconCategoria} alt="" />
                                            <span>{valor.categoria}</span>
                                        </div>
                                    </div>

                                </div>

                                <div className='principal__embaixo'>

                                    <div className='data__hora'>
                                        <div className='data__principal'>
                                            <img src={pinGray} alt="" />
                                            <span>{valor.cidade}, {valor.endereco}</span>
                                        </div>

                                        <p>|</p>

                                        <div className='hora__principal'>
                                            <img src={calendarGray} alt="" />
                                            <span>{valor.dataOcorrencia}</span>
                                        </div>
                                    </div>

                                    <p className='descricao__principal'>{valor.descricaoBreve}</p>

                                </div>

                        </div>


                        <div className='principal__direita'>
                            <div className='button__principal'>
                                <NavLink className='detalhes__principal' to={`/buscar-objetos/${valor.id}`}>Ver detalhes</NavLink>
                                <img className='arrow__button__principal' src={arrowBlue} alt="" />
                            </div>
                        </div>
                    </div>
                )})}
        </div>
        )
}

export default PrincipalCards;