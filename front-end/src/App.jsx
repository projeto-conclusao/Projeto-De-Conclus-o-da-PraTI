import { useState, useEffect} from 'react'; 
import Header from './components/ui/header/Header.jsx';

function App() {

    // alternar tema 
  
    const [tema, setTema] = useState('light');

    function alternarTema(){
        setTema(tema === 'light' ? 'dark' : 'light');
    }

    useEffect(() => {
        document.documentElement.setAttribute('data-theme', tema);

    }, [tema]); // É executado toda vez que o o useState (tema) muda

    // -------------------------------------------------------------------

    return (
        <Header tema={tema} aoAlternarTema={alternarTema} /> 
    )
}

export default App; 
