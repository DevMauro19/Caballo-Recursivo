package Caballos;

public class CaballoRecursivo {

    static final int N = 8; //tamaño del tablero
    static final int[] desplazamientoX = {2, 1, -1, -2, -2, -1, 1, 2}; //movimientos que puede hacer el caballo en x
    static final int[] desplazamientoY = {1, 2, 2, 1, -1, -2, -2, -1}; //movimientos que puede hacer el caballo en y
    //La forma en que se mueve el camballo es una combinación de estos movimientos ejemplo {{2,1},{1,2},{-1,2}} primero en x luego en y
    
    //Clase auxiliar que nos sirve para guardar el tablero como cambia el tablero
    
    //La clase es static, para evitar que cada objeto de Estado necesite una referencia a un objeto externo de Caballo Recursivo
    static class Estado { 
    	
        int[][] tablero; //declarar un tablero temporal
        int paso; //cantidad de pasos que debe recorrer el caballo

        Estado(int[][] tablero, int paso) {
            this.tablero = tablero;
            this.paso = paso;
        }
        
    }

    public static void main(String[] args) {
        int[][] tablero = new int[N][N]; //crear el tablero principal / inicial
        for (int[] fila : tablero)
            java.util.Arrays.fill(fila, -1); //llenar el tablero inicialmente con -1

        tablero[0][0] = 0; //posición en la que va a empezar el caballo
        Estado estadoInicial = new Estado(tablero, 0); //inicar la copia de la primera posición o tablero inicial

        if (!resolverDesde(0, 0, estadoInicial)) {
            System.out.println("No se encontró solución.");
        }
    }
    
    //metodo con el que se busca encontrar el camino que nos lleve a  la solución
    static boolean resolverDesde(int x, int y, Estado estado) { //recibe las posiciones y el tablero
       
    	//Caso base:
    	//Si el caballor ya recorrió todas las casillas del tablero
    	//mostramos la solución encontrada
    	if (estado.paso == N * N - 1) { 
            imprimir(estado.tablero); 
            return true;
        }
        
        for (int i = 0; i < 8; i++) {
            int nx = x + desplazamientoX[i]; //hacia donde nos vamos a mover en x
            int ny = y + desplazamientoY[i]; //hacia donde nos vamos a mover en y

            //verificar si es valido

            if (esValido(nx, ny, estado.tablero)) {
                estado.tablero[nx][ny] = estado.paso + 1; //si es valido nos movemos y sumamos en 1 al numero de pasos
                estado.paso++;
                
                //y volvemos a llamar a resolver haciendo el mismo proceso de resolver desde la nueva posición
                if (resolverDesde(nx, ny, estado))
                    return true;
                
                //por si un movimiento no esta permitido devolvemos en 1 los pasos y volvemos a la posición anterior
                // backtrack
                estado.tablero[nx][ny] = -1;
                estado.paso--;
            }
        }

        return false;
    }
    
    //verificar que el movimiento si sea valido y no nos salgamos del tablero
    static boolean esValido(int x, int y, int[][] tablero) {
        return x >= 0 &&  y >= 0 && x <N && y < N && tablero[x][y] == -1;   //verificar que no se salga del tablero, el tablero[x][y]==-1 es para verificar que no esta recorrida esa posición
    }

    //mostrar el tablero
    static void imprimir(int[][] tablero) {
        for (int[] fila : tablero) {
            for (int celda : fila)
                System.out.printf("%2d ", celda);
            System.out.println();
        }
    }
}
