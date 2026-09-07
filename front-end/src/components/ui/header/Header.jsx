import './Header.css';

import logo from '../../../assets/logo_completa.png'; 
import iconCadastrarObjeto from '../../../assets/icons/cabecalho_cadastrar_objeto.svg'; 
import temaDark from '../../../assets/icons/cabecalho_tema_dark.svg'; 
import temaLight from '../../../assets/icons/cabecalho_tema_light.svg'; 

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
                    <div className='button button__entrar'> 
                        <a  className='entrar link' href="#">Entrar</a> 
                    </div>

                    <div className='button button__cadastrar'> 
                        <img className='cabecalho__icon'src={iconCadastrarObjeto} alt="" /> 
                        <a className='cadastrar link' href="#">Cadastrar objeto</a>                
                    </div>

                    <div className='button button__tema' onClick={aoAlternarTema}>
                        {tema === 'light' ? <img className="tema" src={temaDark} alt="" /> : <img className="tema" src={temaLight} alt="" />}
                    </div>
            </div>

            
        </header>
    )
}

export default Header;