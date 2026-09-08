import { useState } from 'react';
import './Header.css';

import logo from '../../assets/logo_completa.png';
import iconCadastrarObjeto from '../../assets/icons/header/cabecalho_cadastrar_objeto.svg';
import temaDark from '../../assets/icons/header/cabecalho_tema_dark.svg';
import temaLight from '../../assets/icons/header/cabecalho_tema_light.svg';
import burguerWhite from '../../assets/icons/header/cabecalho_burguer_white.svg';
import burguerDark from '../../assets/icons/header/cabecalho_burguer_black.svg';
import closeWhite from '../../assets/icons/header/cabecalho_close_white.svg';
import closeBlack from '../../assets/icons/header/cabecalho_close_black.svg';
import personWhite from '../../assets/icons/header/cabecalho_person_white.png';
import personDark from '../../assets/icons/header/cabecalho_person_dark.png';

function Header({ tema, aoAlternarTema }) {

    // Implementar funcionalidade para reconhecer se usuario está logado ou não (após implementacão do login)

    const [menuOpen, setMenuOpen] = useState(false);

    function toogleMenu() {
        !menuOpen ? setMenuOpen(true) : setMenuOpen(false);
    }

    return (
        <header className='header cabecalho_content'>
            <a href="/"><img className='cabecalho__logo' src={logo} alt="Página inicial" /></a>

            <div className={!menuOpen ? 'menu__desativo' : 'menu__ativo'}>
                {tema === 'light' ? <img className='burguer close__menu' onClick={toogleMenu} src={closeWhite} alt="Fechar menu" /> : <img className='burguer close__menu' onClick={toogleMenu} src={closeBlack} alt="Fechar menu" />}
                <nav>
                    <ul>
                        <li><a className='nav__link' href="#">início</a></li>
                        <li><a className='nav__link' href="#">Buscar objetos</a></li>
                        <li className='link__cadastrar'> <a className='nav__link' href="#">Cadastrar objeto</a> </li>
                        <li><a className='nav__link' href="#">Como funciona</a></li>
                        <li><a className='nav__link' href="#">Achados</a></li>
                        <li><a className='nav__link' href="#">Perdidos</a></li>

                    </ul>
                </nav>
            </div>

            <div className='cabecalho__buttons'>

                <div className='button button__entrar'>
                    <a className='entrar link' href="#">Entrar</a>
                </div>

                <div className='button button__cadastrar'>
                    <img className='cabecalho__icon' src={iconCadastrarObjeto} alt="Cadastrar objeto" />
                    <a className='cadastrar link' href="#">Cadastrar objeto</a>
                </div>

                <div className='button button__tema' onClick={aoAlternarTema}>
                    {tema === 'light' ? <img className="tema" src={temaDark} alt="Ativar tema escuro" /> : <img className="tema" src={temaLight} alt="Ativar tema claro" />}
                </div>

                <div className='button__person'>
                    {tema === 'light' ? <a href="#"> <img className="person" src={personDark} alt="Acessar perfil" /> </a> : <a href="#"> <img className="person" src={personWhite} alt="Acessar perfil" /> </a>}
                </div>

                <div className='burguer__menu' onClick={toogleMenu}>
                    {tema === 'light' ? <img className='burguer' src={burguerDark} alt="Abrir menu" /> : <img className='burguer' src={burguerWhite} alt="" />}
                </div>
            </div>
        </header>
    )
}

export default Header;