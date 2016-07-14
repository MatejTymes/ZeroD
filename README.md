# ZeroD - Zero Downtime automation

## About

Currently the data migration is mostly painful and manual process. As it is not automated it can't be properly tested and as such most products require downtime to verify everything is ok.

To avoid this I've designed migration phases where each can be automated, is understandable and migration between them can be individually tested.

The phases are as follows:

| phase | Reads         | Writes                 |
|:-----:|---------------|------------------------|
| 1.    | old way reads | old way writes         |
| 2.    | old way reads | old and new way writes |
| 3.    | new way reads | old and new way writes |
| 4.    | new way reads | new way writes         |

Each time all application nodes reach a certain phase a migration script for the phase will be triggered (if it exists). Once a migration script has finished nodes will be notified to move onto next phase. This continues until they reach the last one and the final migration script has finished.

## Example scenario

### Splitting field

Imagine you would like to split existing database field `fullName` into two separate fields: `firstName` and `lastName`.

In this case each of our newly deployed application nodes should be able to handle:
- the **old way** - where we read and write `fullName` db column
- the **new way** - where we read and write `firstName` and `lastName` db columns.

Which way should be used for reads and writes will be resolved by ZeroD automatically.

Also we would need to provide scripts that will be executed after all application nodes will reach a specific phase:

| phase | migration script to execute once all nodes reached this phase                                              |
|:-----:|------------------------------------------------------------------------------------------------------------|
| 1.    | create new nullable db columns: `firstName` and `lastName`                                                 |
| 2.    | for each record that misses values for `firstName` and `lastName` populate them using the `fullName` field |
| 3.    | make new db columns `firstName` and `lastName` non-nullable                                                |
| 4.    | delete old db column `fullName`                                                                            |

ZeroD will select which node should execute which script based on the actual stage of all application nodes.

## Usage

ZeroD adds the ability to switch between migration phases and execute migration scripts in an automated way. It also lets application nodes know which read and write approach should be used (old, new or both - both apply only to writes).

Code samples will be added as the library will progress.