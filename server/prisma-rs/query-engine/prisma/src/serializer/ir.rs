//! Serializer Intermediate Representation
//!
//! A flexible serializer IR that parses `PrismaQueryResult`s

use super::Envelope;
use core::{MultiPrismaQueryResult, PrismaQueryResult, SinglePrismaQueryResult};
use prisma_models::PrismaValue;
use std::collections::BTreeMap;

/// A `QueryResponse` is either some data or an error
pub enum ResponseType {
    Data,
    Error,
}

/// A key -> value map to an IR item
pub type Map = BTreeMap<String, Item>;

/// A list of IR items
pub type List = Vec<Item>;

/// An IR item that either expands to a subtype or leaf-node
pub enum Item {
    Map(Map),
    List(List),
    Value(PrismaValue),
}

/// A serialization IR builder utility
pub struct IrBuilder<'results>(Vec<&'results PrismaQueryResult>);

impl<'results> IrBuilder<'results> {
    pub fn new() -> Self {
        Self(vec![])
    }

    /// Add a single query result to the builder
    pub fn add(mut self, q: &'results PrismaQueryResult) -> Self {
        self.0.push(q);
        self
    }

    /// Parse collected queries into an envelope type
    pub fn build(self) -> Envelope {
        unimplemented!()
    }
}

fn build_map(result: &SinglePrismaQueryResult) -> Map {
    // Build selected fields first
    let outer = match &result.result {
        Some(single) => single
            .field_names
            .iter()
            .zip(&single.node.values)
            .fold(Map::new(), |mut map, (name, val)| {
                map.insert(name.clone(), Item::Value(val.clone()));
                map
            }),
        None => panic!("No result found"),
    };

    // Then add nested selected fields
    result.nested.iter().fold(outer, |mut map, query| {
        match query {
            PrismaQueryResult::Single(nested) => map.insert(nested.name.clone(), Item::Map(build_map(nested))),
            PrismaQueryResult::Multi(nested) => map.insert(nested.name.clone(), Item::List(build_list(nested))),
        };

        map
    })
}

fn build_list(result: &MultiPrismaQueryResult) -> List {
    result
        .result
        .as_pairs()
        .iter()
        .map(|vec| {
            Item::Map(vec.iter().fold(Map::new(), |mut map, (name, value)| {
                map.insert(name.clone(), Item::Value(value.clone()));
                map
            }))
        })
        .collect()
}

impl From<Map> for Envelope {
    fn from(map: Map) -> Self {
        Self {
            tt: ResponseType::Data,
            root: Item::Map(map),
        }
    }
}

impl From<List> for Envelope {
    fn from(list: List) -> Self {
        Self {
            tt: ResponseType::Data,
            root: Item::List(list),
        }
    }
}