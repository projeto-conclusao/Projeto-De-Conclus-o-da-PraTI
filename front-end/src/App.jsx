import { useState, useEffect} from 'react'; 
import Header from './components/header/header.jsx';

// Reaproveitamento de estrutura
import { Outlet } from 'react-router';

function App() {
    
    // alternar tema 
  
const [tema, setTema] = useState(() => {
        let preferencia = localStorage.getItem('tema');
        return preferencia || 'light'; 
    });

    function alternarTema(){
        setTema(tema === 'light' ? 'dark' : 'light');
    }

    useEffect(() => {
        document.documentElement.setAttribute('data-theme', tema);
        localStorage.setItem('tema', tema);
        
    }, [tema]); // É executado toda vez que o o useState (tema) muda

    return (
        <div className='app'>
            <header>
                <Header tema={tema} aoAlternarTema={alternarTema} /> 
            </header>

            <main className="content">
                <Outlet context={{ tema }} /> {/* Aqui entra as paginas que estarão no roteador */}
            </main>
        </div>
    )
}

export default App; 
