# Backend Architecture

## Topologia

O backend é um monólito modular com deploy único. Cada bounded context mantém
suas próprias camadas e não acessa controllers, services, repositories ou
models de outro contexto.

Os contextos atuais são `shared`, `iam`, `organizations`, `processes`, `admin`,
`audit` e `workspace`.

## Camadas

Cada contexto de negócio organiza seus componentes em:

- `controller/dto/request` e `controller/dto/response`: entrada e saída HTTP;
- `service`: contratos de aplicação;
- `service/impl`: implementação dos casos de uso;
- `model/domain`: records e objetos puros de domínio;
- `model/entity`: entidades de persistência;
- `repository`: portas/adaptadores de persistência;
- `mapper`: conversão entre DTOs, domínio e entidades;
- `config`: configuração específica do contexto;
- `exception`: exceções específicas do contexto.

## Regra de independência do domínio

Código em `model/domain` não pode importar ou depender de:

- `org.springframework.*`;
- `jakarta.persistence.*`;
- `com.bucket4j.*`.

Essa regra é verificada por `ArchitectureTest` com ArchUnit. A camada de
domínio deve permanecer independente de framework; integrações ficam nas
camadas externas e as entidades JPA permanecem em `model/entity`.

## Código compartilhado

`shared` contém somente abstrações transversais. `BusinessException` e sua
classe base são contratos comuns de erro; códigos concretos pertencem ao
bounded context que define a regra de negócio.
