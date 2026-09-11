import { useContext } from 'react'
import { filterContext } from '../../contexts/buscarObjetosContexts/filterContext.jsx'; 

function useFilter(){

    const context = useContext(filterContext);

    if (!context){ throw new Error('Não existe o contexto Filter') }

    return context;
}

export { useFilter }