# 🎬 CineTicket — Sistema de boletería y reservas de cine

**Autora:** Claudia Patricia Galvis Jiménez  
**Materia:** Ingeniería de Software III — Universidad Libre de Pereira  
**Versión:** 1.0 (Octubre 2025)  
**Lenguaje:** Java 17 + JavaFX + PostgreSQL  
**Metodología:** PSP (Personal Software Process)

---

## 🧩 Descripción General

CineTicket es una aplicación de escritorio desarrollada en **Java 17** con **JavaFX**, que permite a los clientes **consultar la cartelera, seleccionar asientos, comprar entradas y combos de confitería**, simulando el pago y generando un **comprobante en PDF**.  
Los administradores pueden **gestionar la cartelera y generar reportes** de ventas por película, día o confitería.

Cumple con los **requisitos funcionales y no funcionales** definidos en el SRS, priorizando la **seguridad (BCrypt)**, **rendimiento (HikariCP)** y **arquitectura en capas**.

--- 

**Patrones aplicados:**  
DAO • Singleton • Service Layer • MVC Modificado • Value Object

---

## 📚 Tecnologías y Librerías
 
| Categoría                   | Herramientas                        |
|-----------------------------|-------------------------------------|
| **Lenguaje**                | Java 17 LTS                         |
| **IDE**                     | IntelliJ IDEA                       |
| **Framework UI**            | JavaFX 17 + FXML + Scene Builder    |
| **Persistencia**            | JDBC + PostgreSQL 16                |
| **Pool de Conexiones**      | HikariCP                            |
| **Seguridad**               | jBCrypt                             |
| **Logs**                    | SLF4J + Logback                     |
| **PDF**                     | Apache PDFBox                       |
| **Pruebas**                 | JUnit 5 + Mockito + JaCoCo          |
| **Calidad de código**       | Checkstyle + SpotBugs               |
| **Gestión de dependencias** | Maven                               |
| **Control de versiones**    | Git + GitHub                        |

---

## 📋 Requerimientos Funcionales (resumen)

- **RF01:** Publicar películas y horarios  
- **RF02:** Mostrar cartelera  
- **RF03:** Selección de asientos (mapa visual GridPane)  
- **RF04:** Máximo 5 entradas por transacción  
- **RF05:** Pago simulado (PSE/Transferencia)  
- **RF06:** Generación de comprobante PDF  
- **RF07:** Cancelar compra antes de la función  
- **RF08:** Combos predefinidos de confitería  
- **RF09:** Registro/Login con hash seguro  
- **RF10:** Historial de compras  
- **RF11:** Reportes de ventas por día, película y confitería  
- **RF12:** Roles de Usuario y Administrador  


## ⚙️ ****************** Configuración del Entorno ******************

### 1️⃣ Requisitos previos
- **Java 17** o superior  
- **PostgreSQL 16** (base de datos `cineticket`)  
- **IntelliJ IDEA** con **Maven**  
- **Scene Builder** (para editar archivos FXML)

### 2️⃣ Crear la base de datos
Ejecuta en pgAdmin o consola:

```sql
CREATE DATABASE cineticket;
```
Luego ejecuta el script SQL 01 y 03, encontrados en src/main/resources/sql/

01_schema crea todas las tablas, relaciones y lo demas necesario

03_seed Crea algunos datos semilla 


### 3️⃣ Configurar credenciales
Copia el archivo application.properties.example y renómbralo como:

src/main/resources/application.properties

Cambia los datos de acuerdo a tu configuracion
Ejemplo:
db.url = jdbc:postgresql://localhost:5433/cineticket

db.username = postgres

db.password = TuContraseña

db.pool.size.min = 5

db.pool.size.max = 20


### ▶️ Ejecución del Proyecto
Opción A: Desde IntelliJ

Abre el proyecto como Maven Project

Verifica dependencias (Reimport)

Ejecuta com.cineticket.Main

Si usas JavaFX 17, agrega los módulos manualmente si es necesario:

--add-modules=javafx.controls,javafx.fxml

Opción B: Desde consola Maven

mvn clean javafx:run

🧠 Flujo Principal (Resumen)

Usuario inicia sesión o se registra (Registra un nuevo usuario primero)

Consulta cartelera

Selecciona película y funcion

Elige hasta 5 asientos

Añade combos de confitería (Al menos 1)

Simula pago (PSE/Transferencia)

Se genera PDF comprobante

Puede cancelar la compra

📈 Estado Actual del Proyecto

✅ Capas modelo, DAO y servicios principales implementadas
✅ Flujo de compra funcional (UI + PDF + BD)
🧩 Pendiente: completar interfaces secundarias (reportes, gestión, historial, cancelaciones masivas)

📜 Licencia

Proyecto académico — Universidad Libre de Pereira
Uso educativo y no comercial.


