#!/bin/bash

remote_repository="dilaxia"  # nome del repository remoto
branches_to_keep=("public" "sviluppo" "stage" "master")  # rami da mantenere
branches_to_delete=()

# Tutti i nomi dei rami remoti eccetto quelli da mantenere
for branch in $(git ls-remote --tags "$remote_repository" | awk -F/ '{print $3}'); do
  if [[ ! " ${branches_to_keep[@]} " =~ " ${branch} " ]]; then
    branches_to_delete+=("$branch")
  fi
done

# Elimina i rami non desiderati dal repository remoto
for branch in "${branches_to_delete[@]}"; do
  ecgho $branch
  git push "$remote_repository" --delete "$branch"
done