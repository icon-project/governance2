# Network proposal API changes in governance2

## Introduction

This document describes changes in network proposal.

## Changes

Format of Proposals registered after governance2 are different from previously registered proposals.  
Belows are response to the getProposal API according to the previous/new format.

### Previous getProposal response

As shown in result.contents below, before governance2, only one proposal could be registered at a time.  
The proposal type is identified by `type(result.content.type)`.  
If you query the proposals registered before governance2, The response will be in this format.

```json
{
  "jsonrpc": "2.0",
  "id": 1234,
  "result": {
    "contents": {
      "description": "text proposal",
      "title": "text proposal",
      "type": "0x0",
      "value": {
        "value": "text proposal"
      }
    },
    "endBlockHeight": "0x23",
    "id": "0x8e36247b0a439017794c2a6a76bff91b7206c37e6e11cd5e7b8f13efb58c2570",
    "proposer": "hxa8df82e93e8a9cd5325e37289bcd0fbc0a8b4e5e",
    "proposerName": "nodehxa8df82e93e8a9cd5325e37289bcd0fbc0a8b4e5e",
    "startBlockHeight": "0x20",
    "status": "0x1",
    "vote": {
      "agree": {
        "amount": "0x153020c0eaa50f7600001",
        "count": "0x1"
      },
      "disagree": {
        "amount": "0x0",
        "count": "0x0"
      },
      "noVote": {
        "amount": "0x0",
        "count": "0x0"
      }
    }
  }
}
```

### New Network proposal response

In the new format, the type of proposal is list.  
The proposal type is identified by the `name` of the dict element in the `list(result.contents.value.proposals)`.  
For possible values of `name`, refer to the [link](https://github.com/icon-project/governance2#registerproposal).

```json
{
  "jsonrpc": "2.0",
  "id": 1234,
  "result": {
    "contents": {
      "description": "test proposal",
      "title": "test title 2",
      "type": "0x9",
      "value": {
        "data": "[{\"value\": {\"text\": \"hello world\"}, \"name\": \"text\"}]"
      }
    },
    "endBlockHeight": "0x23",
    "id": "0x8e36247b0a439017794c2a6a76bff91b7206c37e6e11cd5e7b8f13efb58c2570",
    "proposer": "hxa8df82e93e8a9cd5325e37289bcd0fbc0a8b4e5e",
    "proposerName": "nodehxa8df82e93e8a9cd5325e37289bcd0fbc0a8b4e5e",
    "startBlockHeight": "0x20",
    "status": "0x1",
    "vote": {
      "agree": {
        "amount": "0x153020c0eaa50f7600001",
        "count": "0x1"
      },
      "disagree": {
        "amount": "0x0",
        "count": "0x0"
      },
      "noVote": {
        "amount": "0x0",
        "count": "0x0"
      }
    }
  }
}
```

### Mapping table(type to name)

Previously, the proposal type was identified by the type value, but from governance2, the proposal type is identified by
name value.  
This table is a mapping for `type` and `name`.

|type|name|
|:---:|:---:|
|0x0|text|
|0x1|revision|
|0x2|maliciousScore|
|0x3|prepDisqualification|
|0x4|stepPrice|
|~~0x5~~|~~irep~~|
|0x6|stepCosts|
|0x7|rewardFund|
|0x8|rewardFundsAllocation|

### Changes in value for old proposals

Some proposal's contents have been changed. Some key in the `value` has been changed or has been deleted.  
Even for proposals registered before governance2.0.0, It will send response **in different format from previous format.**  
Below shows changes for proposals.

**text proposal**  
The `value` key in value has been *changed* to `text`.
```json
{
  "contents": {
    "description": "test register proposal",
    "title": "test register",
    "type": "0x0",
    "value": {
      "text": "hello world"
    }
  }
}
```
**revision proposal**  
`name` key has been *removed* and `code` key has been *changed* to `revision`.
```json
{
  "contents": {
    "description": "set revision 14",
    "title": "set revision 14",
    "type": "0x1",
    "value": {
      "revision": "0xe"
    }
  }
}
```
**step price proposal**  
The `value` key has been *changed* to `stepPrice`.
```json
{
  "contents": {
    "description": "Step price 15000000000",
    "title": "Step price",
    "type": "0x4",
    "value": {
      "stepPrice": "0x37e11d600"
    }
  }
}
```
