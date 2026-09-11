import { createContext, useReducer } from "react";

export const filterContext = createContext();

function reducer(state, action){ // funcão reducer que sabe como atualizar o ESTADO (state)

    if(action.type === 'add'){ 
        return {
           ...state,
           secondaryFilters: action.value
        }  
    
    } else if (action.type === 'change'){  
        return {
           ...state,
           primaryFilters: action.value
        } 
    }

    // obs: cria um novo state do zero a cada mudanca e não pode haver mesmo nome se não sobreescreve
}

function FilterProvider({ children }){

    const [state, dispatch] = useReducer(reducer, { primaryFilters: {}, secondaryFilters: 'TODOS'}); // state: estado atual = []/{} depois do reducer, dispatch: atualiza = reducer()
    // É o state que usamos posteriormente para realizarmos os filtros, pegamos por ele os valores com o hook personalizado criado 
    // que está utilizando o createContext

    return (
        <filterContext.Provider value={ {state, dispatch} } >  {/* passando o value com o state e dispatch como um obj, onde todo os filhos/childrens que estiverem englobados no filterProvider vão acessar esses valores*/}
            { children }
        </filterContext.Provider >
    )
}

export default FilterProvider ;

