# bash completion for wsk                                  -*- shell-script -*-

__debug()
{
    if [[ -n ${BASH_COMP_DEBUG_FILE} ]]; then
        echo "$*" >> "${BASH_COMP_DEBUG_FILE}"
    fi
}

# Homebrew on Macs have version 1.3 of bash-completion which doesn't include
# _init_completion. This is a very minimal version of that function.
__my_init_completion()
{
    COMPREPLY=()
    _get_comp_words_by_ref "$@" cur prev words cword
}

__index_of_word()
{
    local w word=$1
    shift
    index=0
    for w in "$@"; do
        [[ $w = "$word" ]] && return
        index=$((index+1))
    done
    index=-1
}

__contains_word()
{
    local w word=$1; shift
    for w in "$@"; do
        [[ $w = "$word" ]] && return
    done
    return 1
}

__handle_reply()
{
    __debug "${FUNCNAME[0]}"
    case $cur in
        -*)
            if [[ $(type -t compopt) = "builtin" ]]; then
                compopt -o nospace
            fi
            local allflags
            if [ ${#must_have_one_flag[@]} -ne 0 ]; then
                allflags=("${must_have_one_flag[@]}")
            else
                allflags=("${flags[*]} ${two_word_flags[*]}")
            fi
            COMPREPLY=( $(compgen -W "${allflags[*]}" -- "$cur") )
            if [[ $(type -t compopt) = "builtin" ]]; then
                [[ "${COMPREPLY[0]}" == *= ]] || compopt +o nospace
            fi

            # complete after --flag=abc
            if [[ $cur == *=* ]]; then
                if [[ $(type -t compopt) = "builtin" ]]; then
                    compopt +o nospace
                fi

                local index flag
                flag="${cur%%=*}"
                __index_of_word "${flag}" "${flags_with_completion[@]}"
                COMPREPLY=()
                if [[ ${index} -ge 0 ]]; then
                    PREFIX=""
                    cur="${cur#*=}"
                    ${flags_completion[${index}]}
                    if [ -n "${ZSH_VERSION}" ]; then
                        # zsh completion needs --flag= prefix
                        eval "COMPREPLY=( \"\${COMPREPLY[@]/#/${flag}=}\" )"
                    fi
                fi
            fi
            return 0;
            ;;
    esac

    # check if we are handling a flag with special work handling
    local index
    __index_of_word "${prev}" "${flags_with_completion[@]}"
    if [[ ${index} -ge 0 ]]; then
        ${flags_completion[${index}]}
        return
    fi

    # we are parsing a flag and don't have a special handler, no completion
    if [[ ${cur} != "${words[cword]}" ]]; then
        return
    fi

    local completions
    completions=("${commands[@]}")
    if [[ ${#must_have_one_noun[@]} -ne 0 ]]; then
        completions=("${must_have_one_noun[@]}")
    fi
    if [[ ${#must_have_one_flag[@]} -ne 0 ]]; then
        completions+=("${must_have_one_flag[@]}")
    fi
    COMPREPLY=( $(compgen -W "${completions[*]}" -- "$cur") )

    if [[ ${#COMPREPLY[@]} -eq 0 && ${#noun_aliases[@]} -gt 0 && ${#must_have_one_noun[@]} -ne 0 ]]; then
        COMPREPLY=( $(compgen -W "${noun_aliases[*]}" -- "$cur") )
    fi

    if [[ ${#COMPREPLY[@]} -eq 0 ]]; then
        declare -F __custom_func >/dev/null && __custom_func
    fi

    # available in bash-completion >= 2, not always present on macOS
    if declare -F __ltrim_colon_completions >/dev/null; then
        __ltrim_colon_completions "$cur"
    fi
}

# The arguments should be in the form "ext1|ext2|extn"
__handle_filename_extension_flag()
{
    local ext="$1"
    _filedir "@(${ext})"
}

__handle_subdirs_in_dir_flag()
{
    local dir="$1"
    pushd "${dir}" >/dev/null 2>&1 && _filedir -d && popd >/dev/null 2>&1
}

__handle_flag()
{
    __debug "${FUNCNAME[0]}: c is $c words[c] is ${words[c]}"

    # if a command required a flag, and we found it, unset must_have_one_flag()
    local flagname=${words[c]}
    local flagvalue
    # if the word contained an =
    if [[ ${words[c]} == *"="* ]]; then
        flagvalue=${flagname#*=} # take in as flagvalue after the =
        flagname=${flagname%%=*} # strip everything after the =
        flagname="${flagname}=" # but put the = back
    fi
    __debug "${FUNCNAME[0]}: looking for ${flagname}"
    if __contains_word "${flagname}" "${must_have_one_flag[@]}"; then
        must_have_one_flag=()
    fi

    # if you set a flag which only applies to this command, don't show subcommands
    if __contains_word "${flagname}" "${local_nonpersistent_flags[@]}"; then
      commands=()
    fi

    # keep flag value with flagname as flaghash
    if [ -n "${flagvalue}" ] ; then
        flaghash[${flagname}]=${flagvalue}
    elif [ -n "${words[ $((c+1)) ]}" ] ; then
        flaghash[${flagname}]=${words[ $((c+1)) ]}
    else
        flaghash[${flagname}]="true" # pad "true" for bool flag
    fi

    # skip the argument to a two word flag
    if __contains_word "${words[c]}" "${two_word_flags[@]}"; then
        c=$((c+1))
        # if we are looking for a flags value, don't show commands
        if [[ $c -eq $cword ]]; then
            commands=()
        fi
    fi

    c=$((c+1))

}

__handle_noun()
{
    __debug "${FUNCNAME[0]}: c is $c words[c] is ${words[c]}"

    if __contains_word "${words[c]}" "${must_have_one_noun[@]}"; then
        must_have_one_noun=()
    elif __contains_word "${words[c]}" "${noun_aliases[@]}"; then
        must_have_one_noun=()
    fi

    nouns+=("${words[c]}")
    c=$((c+1))
}

__handle_command()
{
    __debug "${FUNCNAME[0]}: c is $c words[c] is ${words[c]}"

    local next_command
    if [[ -n ${last_command} ]]; then
        next_command="_${last_command}_${words[c]//:/__}"
    else
        if [[ $c -eq 0 ]]; then
            next_command="_$(basename "${words[c]//:/__}")"
        else
            next_command="_${words[c]//:/__}"
        fi
    fi
    c=$((c+1))
    __debug "${FUNCNAME[0]}: looking for ${next_command}"
    declare -F "$next_command" >/dev/null && $next_command
}

__handle_word()
{
    if [[ $c -ge $cword ]]; then
        __handle_reply
        return
    fi
    __debug "${FUNCNAME[0]}: c is $c words[c] is ${words[c]}"
    if [[ "${words[c]}" == -* ]]; then
        __handle_flag
    elif __contains_word "${words[c]}" "${commands[@]}"; then
        __handle_command
    elif [[ $c -eq 0 ]] && __contains_word "$(basename "${words[c]}")" "${commands[@]}"; then
        __handle_command
    else
        __handle_noun
    fi
    __handle_word
}

_wsk_action_create()
{
    last_command="wsk_action_create"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--annotation=")
    two_word_flags+=("-a")
    local_nonpersistent_flags+=("--annotation=")
    flags+=("--annotation-file=")
    two_word_flags+=("-A")
    local_nonpersistent_flags+=("--annotation-file=")
    flags+=("--copy")
    local_nonpersistent_flags+=("--copy")
    flags+=("--docker=")
    local_nonpersistent_flags+=("--docker=")
    flags+=("--kind=")
    local_nonpersistent_flags+=("--kind=")
    flags+=("--logsize=")
    two_word_flags+=("-l")
    local_nonpersistent_flags+=("--logsize=")
    flags+=("--main=")
    local_nonpersistent_flags+=("--main=")
    flags+=("--memory=")
    two_word_flags+=("-m")
    local_nonpersistent_flags+=("--memory=")
    flags+=("--native")
    local_nonpersistent_flags+=("--native")
    flags+=("--param=")
    two_word_flags+=("-p")
    local_nonpersistent_flags+=("--param=")
    flags+=("--param-file=")
    two_word_flags+=("-P")
    local_nonpersistent_flags+=("--param-file=")
    flags+=("--sequence")
    local_nonpersistent_flags+=("--sequence")
    flags+=("--timeout=")
    two_word_flags+=("-t")
    local_nonpersistent_flags+=("--timeout=")
    flags+=("--web=")
    local_nonpersistent_flags+=("--web=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_action_delete()
{
    last_command="wsk_action_delete"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_action_get()
{
    last_command="wsk_action_get"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--save")
    local_nonpersistent_flags+=("--save")
    flags+=("--save-as=")
    local_nonpersistent_flags+=("--save-as=")
    flags+=("--summary")
    flags+=("-s")
    local_nonpersistent_flags+=("--summary")
    flags+=("--url")
    flags+=("-r")
    local_nonpersistent_flags+=("--url")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_action_invoke()
{
    last_command="wsk_action_invoke"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--blocking")
    flags+=("-b")
    local_nonpersistent_flags+=("--blocking")
    flags+=("--param=")
    two_word_flags+=("-p")
    local_nonpersistent_flags+=("--param=")
    flags+=("--param-file=")
    two_word_flags+=("-P")
    local_nonpersistent_flags+=("--param-file=")
    flags+=("--result")
    flags+=("-r")
    local_nonpersistent_flags+=("--result")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_action_list()
{
    last_command="wsk_action_list"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--limit=")
    two_word_flags+=("-l")
    local_nonpersistent_flags+=("--limit=")
    flags+=("--name-sort")
    flags+=("-n")
    local_nonpersistent_flags+=("--name-sort")
    flags+=("--skip=")
    two_word_flags+=("-s")
    local_nonpersistent_flags+=("--skip=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_action_update()
{
    last_command="wsk_action_update"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--annotation=")
    two_word_flags+=("-a")
    local_nonpersistent_flags+=("--annotation=")
    flags+=("--annotation-file=")
    two_word_flags+=("-A")
    local_nonpersistent_flags+=("--annotation-file=")
    flags+=("--copy")
    local_nonpersistent_flags+=("--copy")
    flags+=("--docker=")
    local_nonpersistent_flags+=("--docker=")
    flags+=("--kind=")
    local_nonpersistent_flags+=("--kind=")
    flags+=("--logsize=")
    two_word_flags+=("-l")
    local_nonpersistent_flags+=("--logsize=")
    flags+=("--main=")
    local_nonpersistent_flags+=("--main=")
    flags+=("--memory=")
    two_word_flags+=("-m")
    local_nonpersistent_flags+=("--memory=")
    flags+=("--native")
    local_nonpersistent_flags+=("--native")
    flags+=("--param=")
    two_word_flags+=("-p")
    local_nonpersistent_flags+=("--param=")
    flags+=("--param-file=")
    two_word_flags+=("-P")
    local_nonpersistent_flags+=("--param-file=")
    flags+=("--sequence")
    local_nonpersistent_flags+=("--sequence")
    flags+=("--timeout=")
    two_word_flags+=("-t")
    local_nonpersistent_flags+=("--timeout=")
    flags+=("--web=")
    local_nonpersistent_flags+=("--web=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_action()
{
    last_command="wsk_action"
    commands=()
    commands+=("create")
    commands+=("delete")
    commands+=("get")
    commands+=("invoke")
    commands+=("list")
    commands+=("update")

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_activation_get()
{
    last_command="wsk_activation_get"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--last")
    flags+=("-l")
    local_nonpersistent_flags+=("--last")
    flags+=("--summary")
    flags+=("-s")
    local_nonpersistent_flags+=("--summary")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_activation_list()
{
    last_command="wsk_activation_list"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--full")
    flags+=("-f")
    local_nonpersistent_flags+=("--full")
    flags+=("--limit=")
    two_word_flags+=("-l")
    local_nonpersistent_flags+=("--limit=")
    flags+=("--since=")
    local_nonpersistent_flags+=("--since=")
    flags+=("--skip=")
    two_word_flags+=("-s")
    local_nonpersistent_flags+=("--skip=")
    flags+=("--upto=")
    local_nonpersistent_flags+=("--upto=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_activation_logs()
{
    last_command="wsk_activation_logs"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--last")
    flags+=("-l")
    local_nonpersistent_flags+=("--last")
    flags+=("--strip")
    flags+=("-r")
    local_nonpersistent_flags+=("--strip")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_activation_poll()
{
    last_command="wsk_activation_poll"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--exit=")
    two_word_flags+=("-e")
    local_nonpersistent_flags+=("--exit=")
    flags+=("--since-days=")
    local_nonpersistent_flags+=("--since-days=")
    flags+=("--since-hours=")
    local_nonpersistent_flags+=("--since-hours=")
    flags+=("--since-minutes=")
    local_nonpersistent_flags+=("--since-minutes=")
    flags+=("--since-seconds=")
    local_nonpersistent_flags+=("--since-seconds=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_activation_result()
{
    last_command="wsk_activation_result"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--last")
    flags+=("-l")
    local_nonpersistent_flags+=("--last")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_activation()
{
    last_command="wsk_activation"
    commands=()
    commands+=("get")
    commands+=("list")
    commands+=("logs")
    commands+=("poll")
    commands+=("result")

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_api_create()
{
    last_command="wsk_api_create"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apiname=")
    two_word_flags+=("-n")
    local_nonpersistent_flags+=("--apiname=")
    flags+=("--config-file=")
    two_word_flags+=("-c")
    local_nonpersistent_flags+=("--config-file=")
    flags+=("--response-type=")
    local_nonpersistent_flags+=("--response-type=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_api_delete()
{
    last_command="wsk_api_delete"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_api_get()
{
    last_command="wsk_api_get"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--format=")
    local_nonpersistent_flags+=("--format=")
    flags+=("--full")
    flags+=("-f")
    local_nonpersistent_flags+=("--full")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_api_list()
{
    last_command="wsk_api_list"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--full")
    flags+=("-f")
    local_nonpersistent_flags+=("--full")
    flags+=("--limit=")
    two_word_flags+=("-l")
    local_nonpersistent_flags+=("--limit=")
    flags+=("--name-sort")
    flags+=("-n")
    local_nonpersistent_flags+=("--name-sort")
    flags+=("--skip=")
    two_word_flags+=("-s")
    local_nonpersistent_flags+=("--skip=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_api()
{
    last_command="wsk_api"
    commands=()
    commands+=("create")
    commands+=("delete")
    commands+=("get")
    commands+=("list")

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_list()
{
    last_command="wsk_list"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--name-sort")
    flags+=("-n")
    local_nonpersistent_flags+=("--name-sort")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_namespace_get()
{
    last_command="wsk_namespace_get"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--name-sort")
    flags+=("-n")
    local_nonpersistent_flags+=("--name-sort")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_namespace_list()
{
    last_command="wsk_namespace_list"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_namespace()
{
    last_command="wsk_namespace"
    commands=()
    commands+=("get")
    commands+=("list")

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_package_bind()
{
    last_command="wsk_package_bind"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--annotation=")
    two_word_flags+=("-a")
    local_nonpersistent_flags+=("--annotation=")
    flags+=("--annotation-file=")
    two_word_flags+=("-A")
    local_nonpersistent_flags+=("--annotation-file=")
    flags+=("--param=")
    two_word_flags+=("-p")
    local_nonpersistent_flags+=("--param=")
    flags+=("--param-file=")
    two_word_flags+=("-P")
    local_nonpersistent_flags+=("--param-file=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_package_create()
{
    last_command="wsk_package_create"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--annotation=")
    two_word_flags+=("-a")
    local_nonpersistent_flags+=("--annotation=")
    flags+=("--annotation-file=")
    two_word_flags+=("-A")
    local_nonpersistent_flags+=("--annotation-file=")
    flags+=("--param=")
    two_word_flags+=("-p")
    local_nonpersistent_flags+=("--param=")
    flags+=("--param-file=")
    two_word_flags+=("-P")
    local_nonpersistent_flags+=("--param-file=")
    flags+=("--shared=")
    local_nonpersistent_flags+=("--shared=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_package_delete()
{
    last_command="wsk_package_delete"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_package_get()
{
    last_command="wsk_package_get"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--summary")
    flags+=("-s")
    local_nonpersistent_flags+=("--summary")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_package_list()
{
    last_command="wsk_package_list"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--limit=")
    two_word_flags+=("-l")
    local_nonpersistent_flags+=("--limit=")
    flags+=("--name-sort")
    flags+=("-n")
    local_nonpersistent_flags+=("--name-sort")
    flags+=("--skip=")
    two_word_flags+=("-s")
    local_nonpersistent_flags+=("--skip=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_package_refresh()
{
    last_command="wsk_package_refresh"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_package_update()
{
    last_command="wsk_package_update"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--annotation=")
    two_word_flags+=("-a")
    local_nonpersistent_flags+=("--annotation=")
    flags+=("--annotation-file=")
    two_word_flags+=("-A")
    local_nonpersistent_flags+=("--annotation-file=")
    flags+=("--param=")
    two_word_flags+=("-p")
    local_nonpersistent_flags+=("--param=")
    flags+=("--param-file=")
    two_word_flags+=("-P")
    local_nonpersistent_flags+=("--param-file=")
    flags+=("--shared=")
    local_nonpersistent_flags+=("--shared=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_package()
{
    last_command="wsk_package"
    commands=()
    commands+=("bind")
    commands+=("create")
    commands+=("delete")
    commands+=("get")
    commands+=("list")
    commands+=("refresh")
    commands+=("update")

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_property_get()
{
    last_command="wsk_property_get"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--all")
    local_nonpersistent_flags+=("--all")
    flags+=("--apibuild")
    local_nonpersistent_flags+=("--apibuild")
    flags+=("--apibuildno")
    local_nonpersistent_flags+=("--apibuildno")
    flags+=("--cliversion")
    local_nonpersistent_flags+=("--cliversion")
    flags+=("--namespace")
    local_nonpersistent_flags+=("--namespace")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_property_set()
{
    last_command="wsk_property_set"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--namespace=")
    local_nonpersistent_flags+=("--namespace=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_property_unset()
{
    last_command="wsk_property_unset"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--namespace")
    local_nonpersistent_flags+=("--namespace")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_property()
{
    last_command="wsk_property"
    commands=()
    commands+=("get")
    commands+=("set")
    commands+=("unset")

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_rule_create()
{
    last_command="wsk_rule_create"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_rule_delete()
{
    last_command="wsk_rule_delete"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--disable")
    local_nonpersistent_flags+=("--disable")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_rule_disable()
{
    last_command="wsk_rule_disable"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_rule_enable()
{
    last_command="wsk_rule_enable"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_rule_get()
{
    last_command="wsk_rule_get"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--summary")
    flags+=("-s")
    local_nonpersistent_flags+=("--summary")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_rule_list()
{
    last_command="wsk_rule_list"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--limit=")
    two_word_flags+=("-l")
    local_nonpersistent_flags+=("--limit=")
    flags+=("--name-sort")
    flags+=("-n")
    local_nonpersistent_flags+=("--name-sort")
    flags+=("--skip=")
    two_word_flags+=("-s")
    local_nonpersistent_flags+=("--skip=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_rule_status()
{
    last_command="wsk_rule_status"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_rule_update()
{
    last_command="wsk_rule_update"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_rule()
{
    last_command="wsk_rule"
    commands=()
    commands+=("create")
    commands+=("delete")
    commands+=("disable")
    commands+=("enable")
    commands+=("get")
    commands+=("list")
    commands+=("status")
    commands+=("update")

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_sdk_install()
{
    last_command="wsk_sdk_install"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--help")
    flags+=("-h")
    local_nonpersistent_flags+=("--help")
    flags+=("--stdout")
    flags+=("-s")
    local_nonpersistent_flags+=("--stdout")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_sdk()
{
    last_command="wsk_sdk"
    commands=()
    commands+=("install")

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_trigger_create()
{
    last_command="wsk_trigger_create"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--annotation=")
    two_word_flags+=("-a")
    local_nonpersistent_flags+=("--annotation=")
    flags+=("--annotation-file=")
    two_word_flags+=("-A")
    local_nonpersistent_flags+=("--annotation-file=")
    flags+=("--feed=")
    two_word_flags+=("-f")
    local_nonpersistent_flags+=("--feed=")
    flags+=("--param=")
    two_word_flags+=("-p")
    local_nonpersistent_flags+=("--param=")
    flags+=("--param-file=")
    two_word_flags+=("-P")
    local_nonpersistent_flags+=("--param-file=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_trigger_delete()
{
    last_command="wsk_trigger_delete"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_trigger_fire()
{
    last_command="wsk_trigger_fire"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--param=")
    two_word_flags+=("-p")
    local_nonpersistent_flags+=("--param=")
    flags+=("--param-file=")
    two_word_flags+=("-P")
    local_nonpersistent_flags+=("--param-file=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_trigger_get()
{
    last_command="wsk_trigger_get"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--summary")
    flags+=("-s")
    local_nonpersistent_flags+=("--summary")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_trigger_list()
{
    last_command="wsk_trigger_list"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--limit=")
    two_word_flags+=("-l")
    local_nonpersistent_flags+=("--limit=")
    flags+=("--name-sort")
    flags+=("-n")
    local_nonpersistent_flags+=("--name-sort")
    flags+=("--skip=")
    two_word_flags+=("-s")
    local_nonpersistent_flags+=("--skip=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_trigger_update()
{
    last_command="wsk_trigger_update"
    commands=()

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--annotation=")
    two_word_flags+=("-a")
    local_nonpersistent_flags+=("--annotation=")
    flags+=("--annotation-file=")
    two_word_flags+=("-A")
    local_nonpersistent_flags+=("--annotation-file=")
    flags+=("--param=")
    two_word_flags+=("-p")
    local_nonpersistent_flags+=("--param=")
    flags+=("--param-file=")
    two_word_flags+=("-P")
    local_nonpersistent_flags+=("--param-file=")
    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk_trigger()
{
    last_command="wsk_trigger"
    commands=()
    commands+=("create")
    commands+=("delete")
    commands+=("fire")
    commands+=("get")
    commands+=("list")
    commands+=("update")

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

_wsk()
{
    last_command="wsk"
    commands=()
    commands+=("action")
    commands+=("activation")
    commands+=("api")
    commands+=("list")
    commands+=("namespace")
    commands+=("package")
    commands+=("property")
    commands+=("rule")
    commands+=("sdk")
    commands+=("trigger")

    flags=()
    two_word_flags=()
    local_nonpersistent_flags=()
    flags_with_completion=()
    flags_completion=()

    flags+=("--apihost=")
    flags+=("--apiversion=")
    flags+=("--auth=")
    two_word_flags+=("-u")
    flags+=("--cert=")
    flags+=("--debug")
    flags+=("-d")
    flags+=("--insecure")
    flags+=("-i")
    flags+=("--key=")
    flags+=("--verbose")
    flags+=("-v")

    must_have_one_flag=()
    must_have_one_noun=()
    noun_aliases=()
}

__start_wsk()
{
    local cur prev words cword
    declare -A flaghash 2>/dev/null || :
    if declare -F _init_completion >/dev/null 2>&1; then
        _init_completion -s || return
    else
        __my_init_completion -n "=" || return
    fi

    local c=0
    local flags=()
    local two_word_flags=()
    local local_nonpersistent_flags=()
    local flags_with_completion=()
    local flags_completion=()
    local commands=("wsk")
    local must_have_one_flag=()
    local must_have_one_noun=()
    local last_command
    local nouns=()

    __handle_word
}

if [[ $(type -t compopt) = "builtin" ]]; then
    complete -o default -F __start_wsk wsk
else
    complete -o default -o nospace -F __start_wsk wsk
fi

# ex: ts=4 sw=4 et filetype=sh
