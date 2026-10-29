# Fluxo de envio e aprovação da Ordem de Serviço

```mermaid
flowchart TD
    A[Oficina cria OS] --> B[Diagnóstico e inclusão dos serviços]
    B --> C{Há pelo menos um serviço?}
    C -- Não --> X[Envio para aprovação recusado]
    C -- Sim --> D[Persiste status AWAITING_APPROVAL]
    D --> E[Registra tentativa PENDING]
    E --> F[Gera PDF em memória]
    F --> G[Emite token aleatório e persiste somente o hash]
    G --> H[Envia e-mail com PDF e link]

    H --> I{SMTP aceitou o envio?}
    I -- Sim --> J[Marca tentativa SENT]
    I -- Não --> K[Marca tentativa FAILED]
    K --> L[OS continua AWAITING_APPROVAL]
    L --> M[Consulta falha ou solicita reenvio]
    M --> E

    J --> N[Cliente abre link: GET]
    N --> O{Token válido, não usado e dentro da validade?}
    O -- Não --> P[Exibe link inválido ou expirado]
    O -- Sim --> Q[Exibe página de confirmação]
    Q --> R[Cliente confirma: POST]
    R --> S{Consumo atômico do token e transição válida?}
    S -- Não --> T[Exibe conflito ou token inválido]
    S -- Sim --> U[Marca token como usado]
    U --> V[Transiciona OS para IN_EXECUTION]
```

O GET do link não altera a OS; somente o POST explícito confirma a aprovação. As operações de consulta/reenvio da entrega são autenticadas; as rotas públicas se limitam à confirmação por token.
