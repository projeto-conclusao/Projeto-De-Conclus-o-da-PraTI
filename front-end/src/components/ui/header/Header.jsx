import './Header.css';
import logo from '../../../assets/logo_completa.png'; 

function Header({tema, aoAlternarTema}){


    return (
        <header className='header cabecalho_content'>
            <img className='cabecalho__logo' src={logo} alt="" />

            <nav>
                <ul>
                    <li><a className='nav__link' href="#">início</a></li>
                    <li><a className='nav__link' href="#">Buscar objetos</a></li>
                    <li><a className='nav__link' href="#">Como funciona</a></li>
                    <li><a className='nav__link' href="#">Achados</a></li>
                    <li><a className='nav__link' href="#">Perdidos</a></li>
                </ul>
            </nav>

            <div className='cabecalho__buttons'>
                <button><a href="#">Entrar</a></button>
                <button><a href="#">Cadastrar objeto</a></button>
            </div>

        </header>
    )
}

export default Header;