package com.kiwi.vista;

import com.kiwi.Modelo.Empleado;
import com.kiwi.Modelo.Historial;
import com.kiwi.Modelo.Incidencia;
import com.kiwi.Modelo.RankingTO;
import com.kiwi.excepciones.Excepciones;
import com.kiwi.manager.MongodbManager;
import org.bson.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class Main {
    public static MongodbManager mongodbManager = MongodbManager.getInstance();
    private static Empleado empleadoLogueado = null;
    private static boolean start = true;

    public static void main(String[] args) {

        do {
            try {
                if (empleadoLogueado == null){
                    loginEmpleado();
                }

                switch (vistaMenu()) {
                    case 1:
                        insertarEmpleado();
                        break;

                    case 2:
                        updateEmpleado();
                        break;

                    case 3:
                        removeEmpleado();
                        break;

                    case 4:
                        getIncidenciaById();
                        break;

                    case 5:
                        showAllIncidencias();
                        break;

                    case 6:
                        insertIncidencia();
                        break;

                    case 7:
                        showIncidenciasByDestino();
                        break;

                    case 8:
                        showIncidenciasByOrigen();
                        break;

                    case 9:
                        showUltimoAcceso();
                        break;

                    case 10:
                        getRankingByIncidencias();
                        break;

                    case 0:
                        start = false;
                        break;
                    default:
                        System.out.println("Opcion desconocida");
                        break;
                }

            }catch (Excepciones ex){
                System.out.println(ex.getMessage());
            }
        }while (start);
        //cuando termine la aplicacion podremos cerrar base de datos
        mongodbManager.sessionClose();


    }

    public static int vistaMenu(){
        System.out.println("*--- BIENVENIDO ---*");
        System.out.println("1. Nuevo empleado");
        System.out.println("2. Editar perfil");
        System.out.println("3. Eliminar perfil");
        System.out.println("4. Obtener incidencia");
        System.out.println("5. Mostrar incidencias");
        System.out.println("6. Insertar incidencia");
        System.out.println("7. Obtener incidencias por destino");
        System.out.println("8. Obtener incidencias por origen");
        System.out.println("9. Obtener Fecha de ultimo acceso");
        System.out.println("10. Obtener Ranking de empleados");
        System.out.println("0. Salir");

        int opcion = InputAsker.askInt("Seleccione opcion: ");
        return opcion;
    }

    /**
     * funcion que se encarga de pedir datos para usar una funcion que dara de alta a un nuevo empleado
     */
    public static void insertarEmpleado(){
        System.out.println("\n*Registro nuevo empleado*");

        String nombreUsu = InputAsker.askString("Indique User name: ");
        String pass = InputAsker.askString("Indique Password: ");
        String nombre = InputAsker.askString("Indique nombre: ");
        int telefono = InputAsker.askInt("Indique telefono: ");

        //despues de crear el objeto empleado podremos usar funcion que esta en manager
        Empleado empleado = new Empleado(nombreUsu,pass,nombre,telefono);
        mongodbManager.insertEmpleado(empleado);

        System.out.println("Empleado: "+nombre+" creado correctamente \n");
    }

    /**
     * funcion que se encarga de iniciar session, cuando el usuario se loguee con username y pass
     * crearemos un objeto que estara en main para poder usarlo mientras la aplicacion este arrancada
     * setearemos el id porque al iniciar el objeto tiene un campo _id = null
     * esto lo hice por que mongodb ya proporciona un id al registrar un documento en su colleccion
     * @throws Excepciones
     */
    public static void loginEmpleado() throws Excepciones {

        String username = InputAsker.askString("Nombre de usuario: ");
        String pass = InputAsker.askString("Password: ");
        System.out.print("\n");

        if (!mongodbManager.loginEmpleado(username,pass)){
            throw new Excepciones(1);
        }

        Document datos = mongodbManager.datosUsuario(username);
        String _id = datos.getObjectId("_id").toString();
        //username
        //pass
        String nombre = datos.get("nombre").toString();
        int telefono = datos.get("telefono").hashCode();

        Empleado empleado = new Empleado(username,pass,nombre,telefono);
        empleado.set_ID(_id);
        empleadoLogueado = empleado;

        //ahora guardamos historial
        SimpleDateFormat format2 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String fechaHora2 = format2.format(new Date());
        Historial historial = new Historial("I", fechaHora2, empleadoLogueado.getUsername());
        mongodbManager.insertarEvento(historial);
    }


    /**
     * funcion que se encarga de mostrar en modo basico los datos del usuario logueado
     * y pide que campo quiere modificar, dentro de username verificaremos que no este en uso si quiere cambiarlo
     * @throws Excepciones
     */
    public static void updateEmpleado() throws Excepciones {
        System.out.println("\n*Edicion de perfil*");
        System.out.println(empleadoLogueado);

        String campo = InputAsker.askString("Indique que campo quiere cambiar: ");
        switch (campo.toLowerCase()){
            case "username":
                String username = InputAsker.askString("Nuevo user name: ");
                if (username.equals(empleadoLogueado.getUsername())){
                    throw new Excepciones(4);
                }

                if (mongodbManager.ExistUserName(username)){
                    System.out.println("Este nombre de usuario ya esta en uso \n");
                }else {
                    empleadoLogueado.setUsername(username);
                    mongodbManager.updateEmpleado(empleadoLogueado);
                }
                break;

            case "pass":
                String pass = InputAsker.askString("Indique nuevo password: ");
                String confirmacion = InputAsker.askString("Repita la contraseña");
                if (!pass.equals(confirmacion)){
                    throw new Excepciones(2);
                }
                empleadoLogueado.setPass(pass);
                mongodbManager.updateEmpleado(empleadoLogueado);
                break;

            case "nombre":
                String nombre = InputAsker.askString("Nuevo nombre: ");
                empleadoLogueado.setNombre(nombre);
                mongodbManager.updateEmpleado(empleadoLogueado);
                break;

            case "telefono":
                int telf = InputAsker.askInt("Nuevo telefono: ");
                empleadoLogueado.setTelefono(telf);
                mongodbManager.updateEmpleado(empleadoLogueado);
                break;

            default:
                throw new Excepciones(3);
        }

        System.out.println("* Campo actualizado correctamente *");
    }

    /**
     * funcion que se encarga de eliminar la cuenta que esta logueada, cerrara la aplicacion cuando se elimine
     * @throws Excepciones
     */
    public static void removeEmpleado() throws Excepciones {
        String confirmacion = InputAsker.askString("¿Estas seguro que quieres eliminar esta cuenta? \nYES/NO: ");
        if (confirmacion.equalsIgnoreCase("yes") || confirmacion.equalsIgnoreCase("y")){
            String pass = InputAsker.askString("Contrasena: ");
            if (!pass.equals(empleadoLogueado.getPass())){
                throw new Excepciones(5);
            }
            mongodbManager.removeEmpleado(empleadoLogueado);
            System.out.println("**Esta cuenta fue eliminada**");
            start = false;

        }
    }

    public static void getIncidenciaById() throws Excepciones{
        Incidencia incidencia = mongodbManager.getIncidenciaById(empleadoLogueado.getUsername());
        System.out.println(incidencia);
    }

    /**
     * funcion que se encarga de registrar incidencia
     */
    public static void insertIncidencia() {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");

        String fechaHora = format.format(new Date());

        List<Empleado> empleados = mongodbManager.listaEmpleados();
        System.out.println("  **Empleados** ");
        for (Empleado empleado: empleados){
            System.out.println("-- "+empleado.getUsername());
        }
        String origen = empleadoLogueado.getUsername() ;
        String destino =InputAsker.askString("Indique a quien va dirigido esta incidencia: ");

        String detalle = InputAsker.askString("Indique el detalle de esta Incidencia: ");
        String tipo =  InputAsker.askString("Que tipo es: URGENTE/NORMAL: ");

        Incidencia incidencia = new Incidencia(fechaHora,origen,destino,detalle,tipo.toLowerCase());
        mongodbManager.insertIncidencia(incidencia);

        //tambien insertaremos el historial aca si es tipo urgente
        if (tipo.equalsIgnoreCase("urgente")){

            SimpleDateFormat format2 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String fechaHora2 = format2.format(new Date());

            Historial historial = new Historial("U", fechaHora2, empleadoLogueado.getUsername());
            mongodbManager.insertarEvento(historial);
        }

        System.out.println("*Incidencia creada correctamente*\n");
    }

    /**
     * Funcion que se encarga de mostrar todas las incidencias que existen en la bbdd collection incidencias
     */
    public static void showAllIncidencias(){
        List<Incidencia> incidencias = mongodbManager.selectAllIncidencias();
        System.out.println("\n** INCIDENCIAS **");
        for (Incidencia doc : incidencias){
            System.out.println(doc);
        }
        System.out.print("\n");
    }

    /**
     * funcion que se encarga de mostrar las incidencias que estan asignadas a un destino es decir a un usuario
     * @throws Excepciones por si en destino es erroneo o si el destino no dispone de incidencias
     */
    public static void showIncidenciasByDestino() throws Excepciones {

        System.out.println(" **EMPLEADOS**");
        List<Empleado> empleados = mongodbManager.listaEmpleados();
        for (Empleado empleado: empleados){
            System.out.println(" * "+empleado.getUsername());
        }
        String destino = InputAsker.askString("Indique a que destino/empleado quiere ver sus incidencias: ");

        if (!mongodbManager.ExistUserName(destino)){
            throw new Excepciones(8);
        }

        Empleado empleado = mongodbManager.getEmpleadoByUsername(destino);

        List<Incidencia> incidencias = mongodbManager.getIncidenciaByDestino(empleado);
        if (incidencias.isEmpty()){
            throw new Excepciones(9);
        }

        System.out.println("**INCIDENCIAS DE "+empleado.getUsername().toUpperCase()+" **");
        for (Incidencia incidencia: incidencias){
            System.out.println("* "+incidencia);
        }

        System.out.print("\n");

        //guardamos historial cuando consulta incidencias de un empleado
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String fechaHora = format.format(new Date());
        Historial historial = new Historial("E", fechaHora, empleadoLogueado.getUsername());
        mongodbManager.insertarEvento(historial);
    }

    /**
     * funcion que se encarga de mostrar todas las incidencias de un orgen en concreto
     * @throws Excepciones por si el origen no existe o si por si no hay incidencias del origen
     */
    public static void showIncidenciasByOrigen() throws Excepciones {

        System.out.println(" **EMPLEADOS**");
        List<Empleado> empleados = mongodbManager.listaEmpleados();
        for (Empleado empleado: empleados){
            System.out.println(" * "+empleado.getUsername());
        }
        String origen = InputAsker.askString("Indique el origen del que quiere ver sus incidencias: ");

        if (!mongodbManager.ExistUserName(origen)){
            throw new Excepciones(8);
        }

        Empleado empleado = mongodbManager.getEmpleadoByUsername(origen);

        List<Incidencia> incidencias = mongodbManager.getIncidenciaByOrigen(empleado);
        if (incidencias.isEmpty()){
            throw new Excepciones(9);
        }

        System.out.println("** INCIDENCIAS DE "+empleado.getUsername().toUpperCase()+" **");
        for (Incidencia incidencia: incidencias){
            System.out.println("* "+incidencia);
        }
        System.out.print("\n");

        //guardamos historial cuando consulta incidencias de un empleado
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String fechaHora = format.format(new Date());
        Historial historial = new Historial("E", fechaHora, empleadoLogueado.getUsername());
        mongodbManager.insertarEvento(historial);
    }

    /**
     * funcion  que se encarga de mostrar el ultimo acceso de un empleado enconcreto
     */
    public static void showUltimoAcceso() throws Excepciones {
        System.out.println("**EMPLEADOS**");
        List<Empleado> empleados = mongodbManager.listaEmpleados();
        for (Empleado empleado: empleados){
            System.out.println(" * "+empleado.getUsername());
        }
        String emplead = InputAsker.askString("Indique el nombre del empleado que quiere ver su ultimo acceso: ");

        if (!mongodbManager.ExistUserName(emplead)){
            throw new Excepciones(8);
        }
        Empleado empleado = mongodbManager.getEmpleadoByUsername(emplead);

        Historial historial = mongodbManager.getUltimoInicioSesion(empleado);
        System.out.println("El ultimo acceso fue");
        System.out.println(historial.getFechaHora()+"\n");
    }


    /**
     * FUNCION que muestra el ranking de empleados
     */
    public static void getRankingByIncidencias(){
        List<RankingTO> ranking = mongodbManager.getRankingEmpleados();
        System.out.println("** RANKING DE EMPLEADOS **");
        for (RankingTO x: ranking){
            System.out.println(x);
        }
    }

}
