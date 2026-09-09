// Junta as rotas de todos os modulos (modules)

import { createBrowserRouter } from 'react-router';
import App from '../App.jsx';
import NotFoundPage from '../pages/errors/notFound/NotFound.jsx'; 

import routesBuscarObjetos from '../modules/buscar-objetos/buscarObjetos.routes.jsx';
import routesHome from '../modules/home/home.routes.jsx';

const router = createBrowserRouter([
    {
        path: "/",
        element: <App />,
        errorElement: <NotFoundPage />,
        children: [
            routesHome,
            routesBuscarObjetos,

        ]                
    }
])

export default router;