import { useState } from 'react';
import { useFilter } from '../../../../hooks/buscarObjetos/filterHook.jsx';
import { objetoVazio } from '../../../../utils/validators.js';
import { NavLink } from 'react-router'; 


import './principalCards.css'; 

import pinGray from '../../../../assets/icons/buscarObjetos/pin-gray.svg';
import calendarGray from '../../../../assets/icons/buscarObjetos/calendar-gray.svg';
import warningRed from '../../../../assets/icons/buscarObjetos/warning-red.svg';
import checkGreen from '../../../../assets/icons/buscarObjetos/check-green.svg';
import hourglassOrange from '../../../../assets/icons/buscarObjetos/hourglass-orange.svg';
import arrowBlue from '../../../../assets/icons/buscarObjetos/arrow-blue.svg';

// falta adicionar logica de filtrar por periodo

// Vai receber de buscarObjetos.jsx depois
const objetos = [
    {
        id: 1,
        imagem: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRrY_4KkPEHad6YFRowx4F7glLuwXbCigLZPQVC0I1P1pF5EFCB5zYq-8c&s=10',
        nome: 'Bola de Basquete Poker', 
        status: 'PERDIDO',
        categoria: 'Bolas', 
        icon_url: 'https://img.icons8.com/?size=100&id=kK2OkfQGPS4B&format=png&color=737373',
        descricaoBreve: 'Iphone 13 azul com pequenos sinais de uso, encontrada próxima ao Posto 8 da praia de Ipanema verde',
        endereco: 'Ipanema',
        cidade: 'Rio de Janeiro',
        dataOcorrencia: 'Hoje, 12:30'
    },
    {
        id: 2,
        imagem: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRrY_4KkPEHad6YFRowx4F7glLuwXbCigLZPQVC0I1P1pF5EFCB5zYq-8c&s=10',
        nome: 'Bola de Basquete Poker', 
        status: 'ENCONTRADO',
        categoria: 'Bolas', 
        icon_url: 'https://img.icons8.com/?size=100&id=kK2OkfQGPS4B&format=png&color=737373',
        descricaoBreve: 'Iphone 13 azul com pequenos sinais de uso, encontrada próxima ao Posto 8 da praia de Ipanema verde',
        endereco: 'Ipanema',
        cidade: 'Rio de Janeiro',
        dataOcorrencia: 'Hoje, 12:30'
    },
    {
        id: 2,
        imagem: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRrY_4KkPEHad6YFRowx4F7glLuwXbCigLZPQVC0I1P1pF5EFCB5zYq-8c&s=10',
        nome: 'Bola de Basquete Poker', 
        status: 'ENCONTRADO',
        categoria: 'Bolas', 
        icon_url: 'https://img.icons8.com/?size=100&id=kK2OkfQGPS4B&format=png&color=737373',
        descricaoBreve: 'Iphone 13 azul com pequenos sinais de uso, encontrada próxima ao Posto 8 da praia de Ipanema verde',
        endereco: 'Ipanema',
        cidade: 'Rio de Janeiro',
        dataOcorrencia: 'Hoje, 12:30'
    },
    {
        id: 2,
        imagem: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRrY_4KkPEHad6YFRowx4F7glLuwXbCigLZPQVC0I1P1pF5EFCB5zYq-8c&s=10',
        nome: 'Bola de Basquete Poker', 
        status: 'ENCONTRADO',
        categoria: 'Bolas', 
        icon_url: 'https://img.icons8.com/?size=100&id=kK2OkfQGPS4B&format=png&color=737373',
        descricaoBreve: 'Iphone 13 azul com pequenos sinais de uso, encontrada próxima ao Posto 8 da praia de Ipanema verde',
        endereco: 'Ipanema',
        cidade: 'Rio de Janeiro',
        dataOcorrencia: 'Hoje, 12:30'
    },
    {
        id: 2,
        imagem: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRrY_4KkPEHad6YFRowx4F7glLuwXbCigLZPQVC0I1P1pF5EFCB5zYq-8c&s=10',
        nome: 'Bola de Basquete Poker', 
        status: 'ENCONTRADO',
        categoria: 'Bolas', 
        icon_url: 'https://img.icons8.com/?size=100&id=kK2OkfQGPS4B&format=png&color=737373',
        descricaoBreve: 'Iphone 13 azul com pequenos sinais de uso, encontrada próxima ao Posto 8 da praia de Ipanema verde',
        endereco: 'Ipanema',
        cidade: 'Rio de Janeiro',
        dataOcorrencia: 'Hoje, 12:30'
    },
    {
        id: 2,
        imagem: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRrY_4KkPEHad6YFRowx4F7glLuwXbCigLZPQVC0I1P1pF5EFCB5zYq-8c&s=10',
        nome: 'Bola de Basquete Poker', 
        status: 'ENCONTRADO',
        categoria: 'Bolas', 
        icon_url: 'https://img.icons8.com/?size=100&id=kK2OkfQGPS4B&format=png&color=737373',
        descricaoBreve: 'Iphone 13 azul com pequenos sinais de uso, encontrada próxima ao Posto 8 da praia de Ipanema verde',
        endereco: 'Ipanema',
        cidade: 'Rio de Janeiro',
        dataOcorrencia: 'Hoje, 12:30'
    },
    {
        id: 2,
        imagem: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRrY_4KkPEHad6YFRowx4F7glLuwXbCigLZPQVC0I1P1pF5EFCB5zYq-8c&s=10',
        nome: 'Bola de Basquete Poker', 
        status: 'ENCONTRADO',
        categoria: 'Bolas', 
        icon_url: 'https://img.icons8.com/?size=100&id=kK2OkfQGPS4B&format=png&color=737373',
        descricaoBreve: 'Iphone 13 azul com pequenos sinais de uso, encontrada próxima ao Posto 8 da praia de Ipanema verde',
        endereco: 'Ipanema',
        cidade: 'Rio de Janeiro',
        dataOcorrencia: 'Hoje, 12:30'
    },
    {
        id: 2,
        imagem: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRrY_4KkPEHad6YFRowx4F7glLuwXbCigLZPQVC0I1P1pF5EFCB5zYq-8c&s=10',
        nome: 'Carteira de Basquete Poker', 
        status: 'ENCONTRADO',
        categoria: 'Bolas', 
        icon_url: 'https://img.icons8.com/?size=100&id=kK2OkfQGPS4B&format=png&color=737373',
        descricaoBreve: 'Iphone 13 azul com pequenos sinais de uso, encontrada próxima ao Posto 8 da praia de Ipanema verde',
        endereco: 'Ipanema',
        cidade: 'Rio de Janeiro',
        dataOcorrencia: 'Hoje, 12:30'
    },
    {
        id: 2,
        imagem: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRrY_4KkPEHad6YFRowx4F7glLuwXbCigLZPQVC0I1P1pF5EFCB5zYq-8c&s=10',
        nome: 'Bola de Basquete Poker', 
        status: 'ENCONTRADO',
        categoria: 'Bolas', 
        icon_url: 'https://img.icons8.com/?size=100&id=kK2OkfQGPS4B&format=png&color=737373',
        descricaoBreve: 'Iphone 13 azul com pequenos sinais de uso, encontrada próxima ao Posto 8 da praia de Ipanema verde',
        endereco: 'Ipanema',
        cidade: 'Rio de Janeiro',
        dataOcorrencia: 'Hoje, 12:30'
    },
    {
        id: 2,
        imagem: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRrY_4KkPEHad6YFRowx4F7glLuwXbCigLZPQVC0I1P1pF5EFCB5zYq-8c&s=10',
        nome: 'Carteira de Basquete Poker', 
        status: 'ENCONTRADO',
        categoria: 'Bolas', 
        icon_url: 'https://img.icons8.com/?size=100&id=kK2OkfQGPS4B&format=png&color=737373',
        descricaoBreve: 'Iphone 13 azul com pequenos sinais de uso, encontrada próxima ao Posto 8 da praia de Ipanema verde',
        endereco: 'Ipanema',
        cidade: 'Rio de Janeiro',
        dataOcorrencia: 'Hoje, 12:30'
    },
    {
        id: 2,
        imagem: 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRrY_4KkPEHad6YFRowx4F7glLuwXbCigLZPQVC0I1P1pF5EFCB5zYq-8c&s=10',
        nome: 'Carteira de Basquete Poker', 
        status: 'PERDIDO',
        categoria: 'Bolas', 
        icon_url: 'https://img.icons8.com/?size=100&id=kK2OkfQGPS4B&format=png&color=737373',
        descricaoBreve: 'Iphone 13 azul com pequenos sinais de uso, encontrada próxima ao Posto 8 da praia de Ipanema verde',
        endereco: 'Ipanema',
        cidade: 'Rio de Janeiro',
        dataOcorrencia: 'Hoje, 12:30'
    }

]

function PrincipalCards({/* objetos */ inicio, fim}){ 

    const {state} = useFilter();
    const {primaryFilters, secondaryFilters} = state;

    
    function filtros(valor){

        let posicao = 0;

        const categorias = ['OBJETO', 'LOCALIZAÇÃO', 'CATEGORIA', 'PERÍODO'];
        const objetos = ['nome', '', '', 'dataOcorrencia']; 

        const objetosAprovados = {}

        categorias.map((valorCategorias) => {

            if(valorCategorias === 'LOCALIZAÇÃO'){

                if (!primaryFilters[valorCategorias]) { return }

                const input = primaryFilters[valorCategorias].toUpperCase().split(' ');
                const bancoDados1 = valor.cidade.toUpperCase(); 
                const bancoDados2 = valor.endereco.toUpperCase(); 

                // every funciona igual o filter, a diferenca é que o filter retorna um novo array, o every retorna um boolean

                const encontrados = input.every((valorEncontrados) => {
                        return bancoDados1.includes(valorEncontrados) || bancoDados2.includes(valorEncontrados);
                }); 

                objetosAprovados[valorCategorias] = encontrados

            } else if (valorCategorias === 'CATEGORIA'){

                if (!primaryFilters[valorCategorias]) { return }

                const input = primaryFilters[valorCategorias].toUpperCase();
                const bancoDados = valor.categoria.toUpperCase(); 

               if(bancoDados.includes(input)) { objetosAprovados[valorCategorias] = false }
               if(bancoDados.includes(input) ) {objetosAprovados[valorCategorias] = true}

            } else {

                if (!primaryFilters[valorCategorias]) { return }
    
                const input = primaryFilters[valorCategorias].toUpperCase().split(' ');
                const bancoDados = valor[objetos[posicao]].toUpperCase(); 

                const encontrados = input.every((valorEncontrados) => {
                        return bancoDados.includes(valorEncontrados);
                })

                objetosAprovados[valorCategorias] = encontrados

            }   
            
            posicao ++
    
        }); 

        // if a quantidade de objetos preenchidos for a mesma de objetos aprovados, então ele filtra

        const aprovados = []
        const preenchidos = []

        for(let p in primaryFilters){
            if (primaryFilters[p]) {preenchidos.push(p)};
        }

        for(let i in objetosAprovados){        
            if (objetosAprovados[i]) {aprovados.push(i)};
        }

        // se não tiver nenhum preenchido nem aprovado ele filtra pelos secondaryFilters direto
        if(preenchidos.length === aprovados.length){
            if(!secondaryFilters || secondaryFilters === 'TODOS'){ return (valor)}
            if(valor.status === secondaryFilters.slice(0,7)){ return (valor)}
            if(valor.status === secondaryFilters.slice(0,10)){return (valor)}

            // logica dos mais recentes e relevantes

        }
    }

    function verificaIconStatus(status){
            if (status === 'PERDIDO'){ return warningRed  }
            if (status === 'ENCONTRADO') { return checkGreen }
            if (status === 'ANALISE') { return hourglassOrange }
    }

    function adicionaClassNameStatus(status){
            if (status === 'PERDIDO'){ return 'status__red'  }
            if (status === 'ENCONTRADO') { return 'status__green'}
            if (status === 'ANALISE') { return 'status__orange'}
    }

    return (
        <div>
            {objetos.filter((valor) => { return filtros(valor)}).slice(inicio, fim).map((valor) => {
                return (<div className='card__principal'>
                            <img className='image__card ' src={valor.imagem} />

                            <div className='principal__meio'>

                                <div className='principal__encima'>

                                    <h1 className='titulo__principal'>{valor.nome}</h1>

                                    <div className='categoria__status'>
                                        <div className='status__principal'>
                                            <div className={adicionaClassNameStatus(valor.status)}>

                                                <img className="image__principal" src={verificaIconStatus(valor.status)} />
                                                <span>{valor.status}</span>

                                            </div>
                                        </div>

                                        <div className='categoria__principal'>
                                            <img className='icon__categoria'src={valor.icon_url} />
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