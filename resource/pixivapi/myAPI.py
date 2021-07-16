import flask
import json
from flask import request
from pixivpy3 import *
import logging

server = flask.Flask("myPixivAPI")

api = ByPassSniApi()  # Same as AppPixivAPI, but bypass the GFW
api.require_appapi_hosts(hostname="public-api.secure.pixiv.net")
api.set_accept_language('zh-CN')

ERROR_CODE = -1
SUCCESS_CODE = 200
P_ERROR = 10001
NOT_EXIST_PID = -2


def clean_illusts(illusts):
    for i in illusts:
        i['user_name'] = i['user']['name']
        i['user_id'] = i['user']['id']
        i['medium_url'] = i['image_urls']['medium']
        i['large_url'] = i['image_urls']['large']
        if i.page_count == 1:
            i['original_url'] = i.meta_single_page.original_image_url
        else:
            i['original_url'] = i.meta_pages[0].image_urls.original
        i['r18'] = any([tag['name'] == 'R-18' for tag in i['tags']])
        del i['total_view'], i['total_bookmarks'], i['is_bookmarked'], i['visible'], i['is_muted'], \
            i['tools'], i['restrict'], i['create_date'], i['sanity_level'], i['x_restrict'], i['tags'], \
            i['meta_single_page'], i['meta_pages'], i['caption'], i['series'], i['user'], i['image_urls']


def code_to_json(code, message):
    return json.dumps({'code': code, 'message': message}, ensure_ascii=False)


def search_illusts(word, limit):
    if limit is None:
        limit = 30
    limit = int(limit)
    res = []
    s = api.search_illust(word)
    clean_illusts(s.illusts)
    res.extend(s.illusts)
    while len(res) < limit and s.next_url:
        next_s = api.parse_qs(s.next_url)
        s = api.search_illust(**next_s)
        clean_illusts(s.illusts)
        res.extend(s.illusts)
    return res


def search_artist(uid, limit):
    if limit is None:
        limit = 30
    limit = int(limit)
    res = []
    s = api.user_illusts(uid)
    clean_illusts(s.illusts)
    res.extend(s.illusts)
    while len(res) < limit and s.next_url:
        next_s = api.parse_qs(s.next_url)
        s = api.user_illusts(**next_s)
        clean_illusts(s.illusts)
        res.extend(s.illusts)
    return res


# 用refresh_token登录
@server.route('/pixiv/token', methods=['get'])
def login_token():
    token = request.values.get('token')
    if token:
        try:
            if api.refresh_token:
                api.auth(refresh_token=api.refresh_token)
            else:
                api.auth(refresh_token=token)
                print(api.refresh_token)
            return code_to_json(SUCCESS_CODE, '登录成功！')
        except PixivError as e:
            return code_to_json(ERROR_CODE, "登录失败！错误信息：" + e.body)
    else:
        return code_to_json(P_ERROR, "参数错误！")


@server.route('/pixiv/login', methods=['get'])
def login():
    username = request.values.get('name')
    pwd = request.values.get('pwd')
    if username and pwd:
        try:
            if api.refresh_token:
                api.auth(refresh_token=api.refresh_token)
            else:
                api.login(username, pwd)
                print(api.refresh_token)
            return code_to_json(SUCCESS_CODE, '登录成功！')
        except PixivError as e:
            return code_to_json(ERROR_CODE, "登录失败！错误信息：" + e.body)
    else:
        return code_to_json(P_ERROR, "参数错误！")


@server.route('/pixiv/search', methods=['get'])
def search():
    word = request.values.get('word')
    limit = request.values.get('limit')
    if word and (limit is None or limit.isdigit()):
        try:
            res = search_illusts(word, limit)
            return code_to_json(SUCCESS_CODE, res)
        except PixivError as e:
            login()
            return code_to_json(ERROR_CODE, e.body)
    else:
        return code_to_json(P_ERROR, "参数错误！")


@server.route('/pixiv/recommend', methods=['get'])
def recommend():
    try:
        res = api.illust_recommended()
        if res.illusts is None:
            login()
        clean_illusts(res.illusts)
        return code_to_json(SUCCESS_CODE, res.illusts)
    except PixivError as e:
        login()
        return code_to_json(ERROR_CODE, e.body)


@server.route('/pixiv/illust', methods=['get'])
def illust_id():
    pid = request.values.get('pid')
    if pid:
        try:
            res = api.illust_detail(pid)
            if res.error:
                return code_to_json(NOT_EXIST_PID, res.error)
            print(res)
            r = [res.illust]
            clean_illusts(r)
            return code_to_json(SUCCESS_CODE, r[0])
        except PixivError as e:
            return code_to_json(ERROR_CODE, e.body)
    else:
        return code_to_json(P_ERROR, "参数错误！")


@server.route('/pixiv/artist', methods=['get'])
def artist_illust():
    uid = request.values.get('uid')
    limit = request.values.get('limit')
    if uid:
        try:
            res = search_artist(uid, limit)
            return code_to_json(SUCCESS_CODE, res)
        except PixivError as e:
            return code_to_json(ERROR_CODE, e.body)
    else:
        return code_to_json(P_ERROR, "参数错误！")


if __name__ == '__main__':
    server.run(port=8888, host='0.0.0.0')
