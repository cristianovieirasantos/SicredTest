/*
Cenário de Negócio:
Todo dia útil por volta das 6 horas da manhã um colaborador da retaguarda do Sicredi recebe e organiza as informações de 
contas para enviar ao Banco Central. Todas agencias e cooperativas enviam arquivos Excel à Retaguarda. Hoje o Sicredi 
já possiu mais de 4 milhões de contas ativas.
Esse usuário da retaguarda exporta manualmente os dados em um arquivo CSV para ser enviada para a Receita Federal, 
antes as 10:00 da manhã na abertura das agências.

Requisito:
Usar o "serviço da receita" (fake) para processamento automático do arquivo.

Funcionalidade:
0. Criar uma aplicação SprintBoot standalone. Exemplo: java -jar SincronizacaoReceita <input-file>
1. Processa um arquivo CSV de entrada com o formato abaixo.
2. Envia a atualização para a Receita através do serviço (SIMULADO pela classe ReceitaService).
3. Retorna um arquivo com o resultado do envio da atualização da Receita. Mesmo formato adicionando o resultado em uma 
nova coluna.


Formato CSV:
agencia;conta;saldo;status
0101;12225-6;100,00;A
0101;12226-8;3200,50;A
3202;40011-1;-35,12;I
3202;54001-2;0,00;P
3202;00321-2;34500,00;B
...

*/
package SincronizaReceita;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@SpringBootApplication
public class SincronizacaoReceita {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(SincronizacaoReceita.class, args);

        if (!verificaHorario()) {
            System.out.println("\n\nSó é possível fazer a importação antes das 10:00 horas da manhã.");
            System.exit(0);
        }


        if (args.length==0) {
            System.out.println("\n\n=========================================================================================================");
            System.out.println("\nVocê precisa informar como parametro o endereço do arquivo a ser importado como segue o exemplo abaixo:");
            System.out.println("java -jar arquivo.jar \"C:\\receita.cvs\" ");
            System.exit(0);
        }

        if (!diaUtil(new Date())){
            System.out.println("\nA sincronia de dados só pode ser feita em dias úteis, a aplicação será finalizada.");
            System.exit(0);
        }

        if (!existeArquivo(args[0])) {
            System.out.println("\nO arquivo para a sincronia não foi encontrado.");
            System.exit(0);
        }

        System.out.println("\nIniciando sincronização de dados, por favor aguarde...");

        ReceitaService objReceita = new ReceitaService();
        File file = new File(args[0]);
        Scanner sc = null;

        try {
            sc = new Scanner(file);
            /* Escreve o arquivo de retorno*/
            PrintWriter writer = new PrintWriter(new File("retornoSincronia.csv"));

            /* Escreve o cabeçalho do arquivo*/
            StringBuilder sb = new StringBuilder();
            sb.append("agencia;");
            sb.append("conta;");
            sb.append("saldo;");
            sb.append("status;");
            sb.append("retorno;");
            sb.append('\n');
            while (sc.hasNextLine()) {
                String[] valoresLinha = sc.nextLine().split(";");
                if (eNumero(valoresLinha[0])){
                    try {
                        escreveLinha(sb, valoresLinha,
                                    objReceita.atualizarConta(valoresLinha[0], valoresLinha[1].replaceAll("-", ""), Double.parseDouble(valoresLinha[2].replaceAll(",", ".")), valoresLinha[3])
                        );

                        } catch (Exception ex) {
                           escreveLinha(sb,  valoresLinha, false);
                        }
                }

            }
            writer.write(sb.toString());
            writer.close();

            System.out.println("\n======> O arquivo retornoSincronia.csv foi criado na pasta onde esta o arquivo .JAR. <======");
            System.out.println("======> Sincronização feita com sucesso! <======");
            System.out.println("======> Fechando aplicação... <======");
            long wait = Math.round(Math.random() * 2000) + 1000;
            Thread.sleep(wait);
            System.exit(0);
        } catch(Exception ex) {
            System.out.println("Erro ocorrido na sincronização !");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        } finally {
            sc.close();
        }
    }

    private static boolean verificaHorario() {
        Calendar cal = Calendar.getInstance();

        return (cal.get(Calendar.HOUR_OF_DAY)<10);
    }

    private static void escreveLinha(StringBuilder sb, String[] arrStr, Boolean valorQuintaColuna){
        sb.append(arrStr[0]);
        sb.append(';');
        sb.append(arrStr[1]);
        sb.append(';');
        sb.append(arrStr[2]);
        sb.append(';');
        sb.append(arrStr[3]);
        sb.append(';');
        sb.append(valorQuintaColuna);
        sb.append('\n');
    }

    /*Verifica se é dia útil (sem levar em conta os feriados) */
    public static boolean diaUtil(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return ( (cal.get(Calendar.DAY_OF_WEEK)!=1)&&(cal.get(Calendar.DAY_OF_WEEK)!=7) );
    }

    /* Metodo que verifica se o valor String é número ou não */
    private static boolean eNumero(String str) {
        return str.matches("[+-]?\\d*(\\.\\d+)?");
    }

    public static boolean existeArquivo(String strCaminho) {
        if ((strCaminho!=null) && (new File(strCaminho).exists())) {
            return true;
        } else {
            return false;
        }
    }
    
}
